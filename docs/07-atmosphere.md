# Atmosphere: eternal night, fog, darkness, and sound

The horror is carried by three layers:

1. **World rules** — fixed time, no skylight, low ambient light, no mobs, sparse sound.
2. **Client rendering** — fog color and density, a darkening overlay, camera effects, particles.
3. **Sound** — ambient loops, distant stings, and the absence of music at the wrong moment.

Keep the server authoritative. The client may render a fade, but the server decides when the player has actually crossed into The Other.

## Eternal night without freezing the world

`fixed_time: 18000` in the dimension type pins the sky at midnight. Game time still advances: crops grow, furnaces burn, and scheduled tasks run. That is the cleanest match for "forever night, time does pass."

If you want a slow cycle instead, omit `fixed_time` and clamp the time in a server tick handler. That fights vanilla sleep and weather logic; use it only if the visual difference matters.

## Fog

Two Forge events control fog on the client:

- `ViewportEvent.ComputeFogColor` — the RGB color.
- `ViewportEvent.RenderFog` — near/far distances and shape; cancel it to take full control.

```java
@Mod.EventBusSubscriber(modid = TheOther.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FogHandler {
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!isTheOther()) {
            return;
        }
        event.setRed(0.005F);
        event.setGreen(0.005F);
        event.setBlue(0.005F);
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (!isTheOther()) {
            return;
        }
        event.setNearPlaneDistance(2.0F);
        event.setFarPlaneDistance(24.0F);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    private static boolean isTheOther() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null
                && minecraft.level.dimension().location().equals(
                        new ResourceLocation(TheOther.MODID, "the_other"));
    }
}
```

Notes:

- `RenderFog` is cancelable. Cancelling it tells the renderer "use the values I set"; forgetting `setCanceled(true)` can let vanilla overwrite them.
- Fog is not the same as darkness. A black fog hides the horizon; it does not reduce block light. Combine it with `ambient_light` and a dimension effects class.
- `FogShape.SPHERE` vs `FogShape.CYLINDER` changes the shape of the fog boundary. Test both in a wide open area.
- Do not set the far plane to zero; the game may divide by it or render nothing. Use a small positive value.
- Fog events fire every frame. Keep the check cheap: compare a cached dimension id, not a string allocation per frame.

## Dimension effects

`DimensionSpecialEffects` is the data-driven fog/light profile for a dimension. Register it on the mod bus, client side:

```java
@SubscribeEvent
public static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
    event.register(new ResourceLocation(TheOther.MODID, "the_other"), new TheOtherDimensionEffects());
}
```

```java
public final class TheOtherDimensionEffects extends DimensionSpecialEffects {
    public TheOtherDimensionEffects() {
        super(Float.NaN, false, SkyType.NONE, false, true);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float sunHeight) {
        return new Vec3(0.005D, 0.005D, 0.005D);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return true;
    }

    @Override
    public float[] getSunriseColor(float skyAngle, float tickDelta) {
        return null;
    }
}
```

- `SkyType.NONE` removes the sky; the world reads as void rather than a dark blue sky.
- `darkened = true` lowers the ambient contribution.
- `Float.NaN` cloud height disables clouds.
- The `effects` id in `dimension_type/the_other.json` must match `theother:the_other`.
- In 1.20.1 this class does **not** expose `renderSky` or `renderClouds`. For a custom sky, use `RenderLevelStageEvent` or a mixin into `LevelRenderer`; do not copy 1.21 examples.

## The chunk-darkening cinematic

The plan says chunks darken "very fast from the distance to the player", everything becomes black, and the terrain changes. There are three honest ways to build that:

| Approach | Fidelity | Cost | Recommendation |
| --- | --- | --- | --- |
| Screen-space overlay + fog + dimension swap | Good approximation: darkness closes in, then the world changes | Low | Ship this first |
| Post-processing shader (`PostChain`) | Real radial mask, per-pixel | Medium/high; version-sensitive | Add if the pack ships Oculus and the effect is core |
| Per-chunk light updates | Literally darkens chunks | Very high; visible seams and lag | Do not |

### The server side

The server owns the timing. It sends a "start darkening" packet with a duration, then teleports the player when the duration elapses.

```java
public static void startDarkening(ServerPlayer player, int durationTicks) {
    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new DarkenPayload(durationTicks));
    player.server.tell(new TickTask(player.server.getTickCount() + durationTicks, () -> {
        ServerLevel target = player.server.getLevel(THE_OTHER_KEY);
        if (target != null) {
            sendTo(target, player, ARRIVAL_POS);
        }
    }));
}
```

`TickTask` schedules on the server thread. If the mapping differs, keep a small queue in `ServerTickEvent` and decrement a counter; do not sleep the server thread.

### The client overlay

Register a GUI overlay:

```java
@SubscribeEvent
public static void registerOverlays(RegisterGuiOverlaysEvent event) {
    event.registerAboveAll("theother_darkening", new DarkeningOverlay());
}
```

```java
public final class DarkeningOverlay implements IGuiOverlay {
    private long startedAt = -1L;
    private int durationTicks = 60;

    public void start(int durationTicks) {
        this.startedAt = Util.getMillis();
        this.durationTicks = durationTicks;
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        if (startedAt < 0L) {
            return;
        }
        long elapsed = Util.getMillis() - startedAt;
        long total = durationTicks * 50L;
        float alpha = Math.min(1.0F, elapsed / (float) total);
        if (alpha >= 1.0F) {
            startedAt = -1L;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, alpha);
        graphics.blit(DARK_TEXTURE, 0, 0, 0, 0, width, height, width, height);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
```

- `IGuiOverlay#render` signature is `(ForgeGui, GuiGraphics, float, int, int)` in 1.20.1.
- `Util.getMillis()` is real time, not game time. It keeps animating during a lag spike; that is what you want for a cinematic.
- `DARK_TEXTURE` is a black texture, or a radial alpha texture for a circular wipe. A plain black `fill` is cheaper:
  `graphics.fill(0, 0, width, height, (int) (alpha * 255) << 24);`
  Use a texture only when you need a shaped mask.
- Reset `RenderSystem` color and blend state. Leaving them modified breaks the HUD and other mods.
- The overlay is client-only. It must not decide the teleport.
- If the player logs out mid-cinematic, the server timer still fires; the player is teleported when they return. Decide whether to resume or skip; `darkeningStartedAt` in `SavedData` lets you do either.

### A radial wipe without a shader

Draw concentric black rectangles from the screen edge inward, leaving a shrinking transparent circle in the center. It is not per-pixel perfect, but it reads as "the dark is closing in" and costs almost nothing. A 1.20.1 `GuiGraphics` can fill rectangles; a texture with a radial alpha gradient is smoother.

### A shader alternative

If the modpack ships Oculus/Iris, a shader can take a `darkenRadius` uniform and darken everything outside a radius around the camera. That is the highest-fidelity version. It is also a separate deliverable: shader packs are not Java, and they must be tested with the exact Oculus/Iris version in the pack.

## Camera and screen effects

- **Camera shake**: `ViewportEvent.ComputeCameraAngles` exposes yaw, pitch, and roll. Add a small sine-based offset during the cinematic, then reset. Do not shake for more than a few seconds; it makes players sick.
- **Vignette**: a GUI overlay with a pre-authored PNG, or a simple edge gradient. Keep it subtle outside the cinematic.
- **FOV pulse**: `ViewportEvent.ComputeFov` can narrow the FOV for a claustrophobic beat. Restore it immediately after.
- **Screen fade**: the overlay above. Do not stack multiple full-screen overlays from different mods; you will fight over alpha.

## Sound design

### Registering a sound

`sounds.json` at `src/main/resources/assets/theother/sounds.json`:

```json
{
  "ambient.forest_whisper": {
    "category": "ambient",
    "sounds": [
      "theother:ambient/forest_whisper_1",
      "theother:ambient/forest_whisper_2"
    ]
  },
  "music.the_other": {
    "category": "music",
    "sounds": [
      "theother:music/the_other"
    ]
  }
}
```

The OGG files live at:

```text
assets/theother/sounds/ambient/forest_whisper_1.ogg
assets/theother/sounds/ambient/forest_whisper_2.ogg
assets/theother/sounds/music/the_other.ogg
```

Register the `SoundEvent`:

```java
public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TheOther.MODID);

public static final RegistryObject<SoundEvent> FOREST_WHISPER = SOUNDS.register(
        "ambient.forest_whisper",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(TheOther.MODID, "ambient.forest_whisper"))
);
```

- The registry name and the `sounds.json` key must match.
- `createVariableRangeEvent` is the 1.20.1 helper for a sound with no fixed range.
- `sounds.json` is loaded from the resource pack stack. A syntax error produces a log warning and a missing sound, not always a crash.

### Playing a sound

Server-side, positional:

```java
level.playSound(null, pos, ModSounds.FOREST_WHISPER.get(), SoundSource.AMBIENT, 0.6F, 0.8F);
```

Client-side, non-positional (menu, cinematic):

```java
Minecraft.getInstance().getSoundManager().play(
        new SimpleSoundInstance(
                new ResourceLocation(TheOther.MODID, "music.the_other"),
                SoundSource.MUSIC, 0.5F, 1.0F, SoundInstance.createUnseededRandom(),
                false, 0, SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true
        )
);
```

`SimpleSoundInstance` has several constructors in 1.20.1. If the mapped signature differs, use `SimpleSoundInstance.forUI(SoundEvent, float volume)` or `forMusic(SoundEvent)` where available. The important part is the sound source and the attenuation; a non-positional cinematic sound should not pan with the camera.

### Ambient loop

`ClientTickEvent` + a timer:

```java
private static int whisperTimer;

@SubscribeEvent
public static void onClientTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END) {
        return;
    }
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null || minecraft.isPaused()) {
        return;
    }
    if (!isForest(minecraft.level)) {
        return;
    }
    if (--whisperTimer > 0) {
        return;
    }
    whisperTimer = 600 + minecraft.level.random.nextInt(1200);
    // play a quiet, non-positional whisper
}
```

- Do not play a sound every tick. A 30–90 second interval is enough.
- `minecraft.isPaused()` avoids playing under the pause menu.
- Randomize pitch and volume slightly; identical repeats become noticeable.
- Keep the ambient bed quiet. The scariest sound is the one the player is not sure they heard.

### Music

- For menu music, replace the vanilla OGGs under `assets/minecraft/sounds/music/menu/` (see [Title screen](02-title-screen.md)).
- For dimension music, a client manager that starts a track on dimension change and stops it on leave is more reliable than trying to hook `MusicManager` internals.
- Do not start music during the darkening; silence is stronger.

## Particles

- Register a `ParticleType` on the mod bus, a `ParticleProvider` on the client (`RegisterParticleProvidersEvent`), and spawn with `level.addParticle(...)`.
- Use particles sparingly: ash, smoke, rose petals. A dense particle field is a performance trap and reads as fireworks, not horror.
- For the forest's "ghosty smoke", a slow, large, low-alpha smoke particle near the ground is more effective than a screen full of particles.

## Performance

- Cache the dimension check per frame; do not allocate a `ResourceLocation` every frame.
- Do not create textures, buffers, or `PoseStack`s in `render`.
- Reset `RenderSystem` state after custom rendering.
- Test with Embeddium/Oculus enabled. A shader that works in vanilla can break under Oculus, and vice versa.
- The darkening overlay is cheap; the fog is cheap; a full-screen shader is not. Profile before adding a post chain.
- On low-end machines, offer a config toggle for the cinematic and the fog. Accessibility matters; not every player can tolerate camera shake or rapid fades.

## Accessibility and restraint

- Provide a config option to disable camera shake, the rapid fade, and the loud stings.
- Do not flash the screen. A slow fade is more unsettling and safer.
- Subtitles are generated from `sounds.json`; give every sound a translation key under `subtitles.theother.*` so deaf players get the cue.
- Use `SoundSource.AMBIENT` for the bed and `SoundSource.MASTER` for stings; respect the player's volume sliders.

## Verification

- [ ] The Other is black at the horizon; the fog does not show the sky.
- [ ] The forest is night at all times, but crops still grow.
- [ ] The darkening overlay starts once, reaches black, and the teleport happens at the end.
- [ ] Relogging mid-cinematic does not leave a permanently black screen.
- [ ] The HUD is visible after the overlay (RenderSystem state reset).
- [ ] The ambient loop does not double-play on dimension change.
- [ ] Subtitles show a readable line for every custom sound.
- [ ] The effect is tolerable with camera shake disabled.
- [ ] The GitHub Actions build is green and the artifact launches.

## Sources

- Forge 1.20.1 `ViewportEvent`: <https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/client/event/ViewportEvent.java>
- Forge 1.20.1 `RegisterGuiOverlaysEvent`: <https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/client/event/RegisterGuiOverlaysEvent.java>
- Forge 1.20.1 `RegisterDimensionSpecialEffectsEvent`: <https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/client/event/RegisterDimensionSpecialEffectsEvent.java>
- Minecraft Wiki, fog: <https://minecraft.wiki/w/Fog>
- Minecraft Wiki, sounds.json: <https://minecraft.wiki/w/Sounds.json>
- ModDevGradle Legacy: <https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md>
