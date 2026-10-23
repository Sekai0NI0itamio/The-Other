# Changing the Minecraft title screen (1.20.1 Forge)

There are three levels of control. Pick the smallest one that achieves the effect.

| Approach | Changes | Code required | Best for |
| --- | --- | --- | --- |
| Resource pack | Panorama, logo, edition badge, splash lines, menu music, language | No | A modpack-wide art direction, and the fastest iteration loop |
| Forge event + custom `Screen` | Layout, buttons, animations, overlays, conditional screens | Yes | The Other's actual menu: dark, custom, part of the mod |
| Mixin into `Minecraft` / `TitleScreen` | Replace or alter the vanilla title screen at its source | Yes | Last resort when an event cannot express the change |

This repository already implements the middle option. The event hook is in [`TheOtherClient`](../src/main/java/io/github/sekai0ni0itamio/theother/client/TheOtherClient.java); the screen is in [`TheOtherTitleScreen`](../src/main/java/io/github/sekai0ni0itamio/theother/client/TheOtherTitleScreen.java).

## 1. Resource pack: the no-code route

A resource pack can replace the vanilla title screen art without touching Java. This is the recommended way to art-direct a **modpack**, because packs can be stacked and disabled by the player.

### Pack skeleton

```text
TheOtherTitlePack/
├── pack.mcmeta
└── assets/minecraft/
    ├── textures/gui/title/
    │   ├── background/
    │   │   ├── panorama_0.png
    │   │   ├── panorama_1.png
    │   │   ├── panorama_2.png
    │   │   ├── panorama_3.png
    │   │   ├── panorama_4.png
    │   │   └── panorama_5.png
    │   ├── mojangstudios.png
    │   └── edition.png
    ├── texts/splashes.txt
    └── sounds/music/menu/
        ├── menu1.ogg
        ├── menu2.ogg
        ├── menu3.ogg
        └── menu4.ogg
```

`pack.mcmeta` for 1.20.1:

```json
{
  "pack": {
    "pack_format": 15,
    "description": "The Other title screen"
  }
}
```

### Panorama

`panorama_0.png` through `panorama_5.png` are the six faces of a cube. Vanilla uses square images; keep them square and power-of-two (1024×1024 is the vanilla size). A non-square image stretches or clips.

To capture a panorama from a world, stand where the camera should be and use a panorama screenshot tool or a mod; the six faces must line up at the edges. Replacing only one face produces a visible seam.

### Logo and edition badge

- `assets/minecraft/textures/gui/title/mojangstudios.png` replaces the Mojang logo.
- `assets/minecraft/textures/gui/title/edition.png` replaces the "Java Edition" badge.

Keep transparency and the same aspect ratio as the vanilla textures, or the layout will look off.

### Splash lines

`assets/minecraft/texts/splashes.txt` is a plain text file with one splash line per line. Replace it with lines that fit the horror tone:

```text
Do not open, or else
It is still watching
One life
The house was not empty
```

The splash text is rendered by `SplashManager`, which reads this file from the resource pack stack.

### Menu music

The vanilla menu music is a set of OGG files under `assets/minecraft/sounds/music/menu/`. Overriding those paths changes the menu music for anyone using the pack. This is simpler and more compatible than trying to stop and restart `MusicManager` from code.

For a custom track that is **not** a vanilla replacement, register a `SoundEvent` and play it from a client tick handler or a custom screen. Do not play audio from the mod constructor.

### Forge loading screen

Do not confuse the title screen with Forge's loading screen. Forge reads `config/splash.properties` in the instance for the loading-screen background, logo offset, fade, and related options. That file does not control the title screen panorama or the `splashes.txt` lines.

### When to use this route

Use it for art. Do not use it to implement gameplay (a "one life" warning screen that blocks buttons, for example); resource packs cannot run code.

## 2. Forge event + custom `Screen`: the code route

### The hook

`ScreenEvent.Opening` fires on the Forge bus on the client before any screen is opened. It exposes:

- `getCurrentScreen()` — the screen that is currently open, possibly `null`
- `getNewScreen()` — the screen that will open, possibly `null`
- `setNewScreen(Screen)` — replace it
- `setCanceled(true)` — prevent it from opening (not used here)

The handler:

```java
@Mod.EventBusSubscriber(modid = TheOther.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TheOtherClient {
    private TheOtherClient() {
    }

    @SubscribeEvent
    public static void replaceTitleScreen(ScreenEvent.Opening event) {
        if (event.getNewScreen() != null && event.getNewScreen().getClass() == TitleScreen.class) {
            event.setNewScreen(new TheOtherTitleScreen());
        }
    }
}
```

Why `getClass() == TitleScreen.class` instead of `instanceof TitleScreen`:

- Another mod may open a **subclass** of `TitleScreen` with its own layout or logic. `instanceof` would replace it and silently break that mod.
- The exact check means "replace only the vanilla menu, and only when nobody else has already replaced it."
- If you want to win over other title-screen mods deliberately, use `instanceof` and document the conflict. Do not do that by accident.

Why the handler is in `client/` and annotated `Dist.CLIENT`:

- `ScreenEvent.Opening` and `Screen` are client classes.
- A dedicated server must not load this class. The `value = Dist.CLIENT` annotation keeps it out of the server's class-loading path.
- `bus = Bus.FORGE` is required: screen events are gameplay events, not mod-lifecycle events.

### The screen

A minimal but complete 1.20.1 screen:

```java
public final class TheOtherTitleScreen extends Screen {
    private static final int TOP_COLOR = 0xFF1A0303;
    private static final int BOTTOM_COLOR = 0xFF000000;
    private static final int TITLE_COLOR = 0xFFE8E8E8;
    private static final int SUBTITLE_COLOR = 0xFF8A8A8A;

    public TheOtherTitleScreen() {
        super(Component.translatable("screen.theother.title"));
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 100;
        int top = this.height / 4 + 48;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("menu.singleplayer"),
                        button -> this.minecraft.setScreen(new SelectWorldScreen(this)))
                .bounds(left, top, 200, 20)
                .build());

        // Multiplayer, Options, Quit follow the same pattern.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.fillGradient(0, 0, this.width, this.height, TOP_COLOR, BOTTOM_COLOR);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 4, TITLE_COLOR);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
```

Key details:

- `Screen` is constructed with a title `Component`. The constructor must not touch `this.minecraft`; it is assigned later, when the screen is opened.
- `init()` is called on open and on every resize. Create widgets here, not in the constructor. `this.width` and `this.height` are current.
- `addRenderableWidget` both registers the widget for rendering and adds it to the input handlers.
- `renderBackground(graphics)` in 1.20.1 draws the vanilla dirt background when no level is loaded. The gradient then covers it.
- `fillGradient(x1, y1, x2, y2, colorStart, colorEnd)` takes six integers in 1.20.1.
- `super.render(...)` draws the widgets and their tooltips. Call it after the background.
- `isPauseScreen()` should be `false` for a title screen; a pause screen tells the game to freeze the world and show the pause menu.
- The screen is recreated on every `TitleScreen` open, including returning from a world. State that must survive (music, animation time) belongs in a static client manager, not in the screen.

### The 1.20.1 import trap

The screen classes moved into subpackages:

```java
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
```

Copying a 1.19 or 1.20.2 tutorial often produces `cannot find symbol` for `SelectWorldScreen` and `JoinMultiplayerScreen`. The compiler error points at the import; fix the package, not the class name. The first CI build of this repository failed on exactly this and was fixed in commit `f77d849`.

### Buttons and navigation

The standard destinations:

| Button | Screen |
| --- | --- |
| Singleplayer | `new SelectWorldScreen(this)` |
| Multiplayer | `new JoinMultiplayerScreen(this)` |
| Options | `new OptionsScreen(this, this.minecraft.options)` |
| Quit | `this.minecraft.stop()` |

The parent `this` matters: the child screen's "back" button returns to the custom title screen, not to vanilla's.

If Forge's mod-list button is missing because you replaced the vanilla screen, add it deliberately:

```java
this.addRenderableWidget(Button.builder(
                Component.translatable("menu.mods"),
                button -> this.minecraft.setScreen(new net.minecraftforge.client.gui.ModListScreen(this)))
        .bounds(left, top + 96, 200, 20)
        .build());
```

Check the class name and constructor against the pinned mappings before relying on it; Forge has moved this screen between versions.

### Adding a background image

```java
private static final ResourceLocation BACKGROUND =
        new ResourceLocation(TheOther.MODID, "textures/gui/title/background.png");

@Override
public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    graphics.blit(BACKGROUND, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
    super.render(graphics, mouseX, mouseY, partialTick);
}
```

The texture lives at `src/main/resources/assets/theother/textures/gui/title/background.png`. The 9-argument `blit` stretches the full texture to the screen. If the image distorts, compute a scaled destination rectangle instead of stretching.

### Adding the vanilla panorama

Vanilla builds a `CubeMap` from `assets/minecraft/textures/gui/title/background/panorama`. A custom screen can construct one:

```java
private CubeMap panorama;

@Override
protected void init() {
    this.panorama = new CubeMap(new ResourceLocation("minecraft", "textures/gui/title/background/panorama"));
    // widgets...
}

@Override
public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    this.panorama.render(this.minecraft, 1.0F, 1.0F, 1.0F);
    // ...
}
```

`CubeMap` is in `net.minecraft.client.renderer`. Its render parameters are a `Minecraft` instance and three floats; check the mapped signature in your IDE before using it. A static image is easier to control and cheaper; use the cube map only if the rotating panorama is part of the mood.

### Logo rendering

`LogoRenderer` (in `net.minecraft.client.gui.components`) can draw the Mojang logo with the vanilla animation:

```java
private final LogoRenderer logoRenderer = new LogoRenderer(false);

// inside render(...)
this.logoRenderer.renderLogo(graphics, this.width, 1.0F);
```

`false` means "not part of a texture atlas" in the vanilla constructor. If the constructor signature differs, use a `blit` of `textures/gui/title/mojangstudios.png` instead. A custom logo is often better for a horror mod than the Mojang logo.

### Conditional screens

A title screen can react to state, but client state is not authoritative. For example, showing a "the world is locked" banner should be driven by a server-synced flag, not by reading a local file from the screen. Keep the screen dumb: render what a client-side state object says, and let the server decide the truth.

## 3. Mixin: replacing the title screen at the source

Use this only if `ScreenEvent.Opening` cannot do the job. The event covers nearly everything a title screen needs.

### Enable Mixin in this build

ModDevGradle Legacy has a mixin helper. Add to `build.gradle`:

```groovy
mixin {
    add sourceSets.main, "${mod_id}.refmap.json"
    config "${mod_id}.mixins.json"
}

dependencies {
    annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'
}

jar {
    manifest.attributes([
            "MixinConfigs": "${mod_id}.mixins.json"
    ])
}
```

Create `src/main/resources/theother.mixins.json`:

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "io.github.sekai0ni0itamio.theother.mixin",
  "compatibilityLevel": "JAVA_17",
  "refmap": "theother.refmap.json",
  "client": [
    "MinecraftMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

Create the mixin:

```java
package io.github.sekai0ni0itamio.theother.mixin;

import io.github.sekai0ni0itamio.theother.client.TheOtherTitleScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "setInitialScreen", at = @At("TAIL"))
    private void theother$replaceTitle(CallbackInfo ci) {
        Minecraft self = (Minecraft) (Object) this;
        self.setScreen(new TheOtherTitleScreen());
    }
}
```

Notes:

- `setInitialScreen` is private in 1.20.1 and takes arguments (`RealmsClient`, `ReloadInstance`, `GameConfig.QuickPlayData`). If you inject into it, match the full descriptor or let Mixin resolve by name with `remap = true`. The safest is to inject at `TAIL` and ignore the arguments with a `CallbackInfo`-only method; Mixin allows this for simple injections, but verify against the mapped method.
- A mixin into `Minecraft#setScreen` is more fragile: it fires for every screen and can recurse.
- Client mixins belong in the `"client"` array. Common mixins go in `"mixins"`.
- A missing refmap produces a warning or a crash depending on the environment. Run the CI build and check the `jar` task output; do not ship a mixin jar without the refmap.
- Mixins are not a security boundary. Do not use them to hide the one-life logic from the client (see [One life](06-one-life.md)).

## 4. Music, splash, and window details

- **Menu music**: replace the OGG files under `assets/minecraft/sounds/music/menu/`. For custom music, register a `SoundEvent` and play it from a client manager.
- **Splash lines**: `assets/minecraft/texts/splashes.txt`.
- **Window title**: `Minecraft.getInstance().getWindow().setTitle("The Other")` from client setup. Check `Window#setTitle` in the mapped sources; it is a client-only call.
- **Window icon**: the vanilla window icon comes from the launcher; changing it requires platform-specific window APIs and is not worth the compatibility risk for a mod.

## 5. Verification checklist

Run through this before calling the screen done:

- [ ] The custom screen appears on first launch.
- [ ] Singleplayer opens the world list; Back returns to the custom screen.
- [ ] Multiplayer opens the server list; Back returns.
- [ ] Options opens and closes; the game does not crash on video settings.
- [ ] Quit exits cleanly.
- [ ] Resizing the window reflows the widgets and does not leave stale positions.
- [ ] GUI scale Auto, 1, and 3 all look acceptable.
- [ ] Returning from a world shows the custom screen again.
- [ ] A dedicated server starts without loading `TheOtherClient` or `TheOtherTitleScreen`.
- [ ] Other title-screen mods are either compatible or documented as conflicting.
- [ ] The GitHub Actions `Build` workflow is green; the jar is downloadable as an artifact.

## 6. Common failures

| Symptom | Cause | Fix |
| --- | --- | --- |
| `cannot find symbol: SelectWorldScreen` | Wrong package in 1.20.1 | Import `...screens.worldselection.SelectWorldScreen` |
| `cannot find symbol: JoinMultiplayerScreen` | Wrong package in 1.20.1 | Import `...screens.multiplayer.JoinMultiplayerScreen` |
| Screen is blank or buttons do nothing | `init()` was not overridden, or widgets were added in the constructor | Build widgets in `init()` |
| Crash on dedicated server | Client class loaded on the server | Move to `client/`, add `value = Dist.CLIENT` |
| Custom screen replaces another mod's menu | `instanceof TitleScreen` | Use exact class comparison |
| Panorama has seams | Images are not a matched cube | Regenerate all six faces from one camera |
| Splash lines do not change | Wrong pack format or wrong path | `pack_format: 15`, `assets/minecraft/texts/splashes.txt` |
| Mixin does nothing | Refmap/config missing, or wrong method descriptor | Check the CI jar, the manifest, and the mixin log |

## Sources

- Forge 1.20.1 `ScreenEvent`: <https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/client/event/ScreenEvent.java>
- Forge 1.20.1 documentation: <https://docs.minecraftforge.net/en/1.20.1/>
- Minecraft Wiki, title screen: <https://minecraft.wiki/w/Title_screen>
- Minecraft Wiki, resource pack: <https://minecraft.wiki/w/Resource_pack>
- ModDevGradle Legacy, mixins: <https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md>
