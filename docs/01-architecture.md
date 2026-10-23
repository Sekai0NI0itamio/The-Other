# Architecture: how a Forge 1.20.1 mod is put together

## The shape of this repository

```text
The Other/
├── build.gradle                    buildscript, run configs, metadata generation
├── gradle.properties               versions and mod metadata (single source of truth)
├── settings.gradle                 plugin repositories and project name
├── src/main/java/                  mod code
│   └── io/github/sekai0ni0itamio/theother/
│       ├── TheOther.java           common entry point, @Mod
│       └── client/                 client-only code, never loaded on a dedicated server
├── src/main/resources/             everything shipped inside the jar
│   ├── assets/theother/            models, textures, lang, sounds (client)
│   └── data/theother/              dimensions, biomes, loot, tags, recipes (server)
├── src/main/templates/             files expanded by generateModMetadata
├── src/generated/resources/        output of the data generator
└── .github/workflows/build.yml     the only build that matters: CI
```

## The entry point

```java
@Mod(TheOther.MODID)
public final class TheOther {
    public static final String MODID = "theother";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TheOther() {
        LOGGER.info("The Other is loading");
    }
}
```

Rules that keep this class healthy:

- The constructor runs **very early**. Do not touch a `Level`, a `ServerPlayer`, or a `Minecraft` instance here. They do not exist yet or are not safe to use.
- Register deferred registers on the **mod event bus**, not the Forge bus.
- Keep the constructor boring. Wire subsystems into small `register(...)` methods.
- Use `MODID` everywhere. Never repeat the string `"theother"` in `ResourceLocation`s.

## Two event buses

Forge 1.20.1 has two buses, and using the wrong one is the most common beginner bug.

| Bus | How to get it | What belongs there |
| --- | --- | --- |
| Mod bus | `FMLJavaModLoadingContext.get().getModEventBus()` | Lifecycle and registration: `FMLCommonSetupEvent`, `FMLClientSetupEvent`, `RegisterEvent`, `BuildCreativeModeTabContentsEvent`, `GatherDataEvent`, `RegisterDimensionSpecialEffectsEvent` |
| Forge bus | `MinecraftForge.EVENT_BUS` | Gameplay: `ServerStartedEvent`, `PlayerEvent`, `LivingDeathEvent`, `BlockEvent`, `TickEvent`, `ScreenEvent` |

Two ways to subscribe:

```java
// Explicit registration, useful when the listener is an object with state
MinecraftForge.EVENT_BUS.register(this);

// Annotation, useful for static handlers; choose the correct bus and side
@Mod.EventBusSubscriber(modid = TheOther.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TheOtherClient {
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        // ...
    }
}
```

`@Mod.EventBusSubscriber` with `value = Dist.CLIENT` prevents the class from loading on a dedicated server. `bus = Bus.MOD` is required for registration events; the default is `Bus.FORGE`.

## Sides

| Side | Meaning | What may run |
| --- | --- | --- |
| `Dist.CLIENT` | The physical client, including singleplayer | Rendering, screens, sounds, input, `Minecraft` |
| `Dist.DEDICATED_SERVER` | A headless server | World logic, progression, one-life checks, networking |
| Logical client | The client thread, even in singleplayer | Client-only state and rendering |
| Logical server | The integrated server in singleplayer, or a dedicated server | World state and authoritative gameplay |

Practical consequences:

- A class that imports `net.minecraft.client.*` must live in `client/` and be guarded by `Dist.CLIENT`.
- A dedicated server must never be required to load a client class. If it does, startup crashes with `NoClassDefFoundError`.
- Singleplayer is **not** a reason to skip server-side checks. The integrated server is still the authority for the world.
- Use `@Mod.EventBusSubscriber(value = Dist.CLIENT)` or `DistExecutor` for client hooks; do not call client code from common code.

## Registries

Forge 1.20.1 uses `DeferredRegister` and `RegistryObject`. Registration is deferred until the registry event, so the register object must be kept alive and registered on the mod bus.

```java
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TheOther.MODID);

    public static final RegistryObject<Item> ENCHANTED_ROSE =
            ITEMS.register("enchanted_rose", () -> new Item(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
```

Common registries for this mod:

| Content | Forge registry | Notes |
| --- | --- | --- |
| Items | `ForgeRegistries.ITEMS` | The rose, the pot item if it is an item |
| Blocks | `ForgeRegistries.BLOCKS` | Statue, pot, custom trapdoor |
| Block items | `ForgeRegistries.ITEMS` | One `BlockItem` per placeable block |
| Block entities | `ForgeRegistries.BLOCK_ENTITY_TYPES` | Chest/sign behaviour if custom |
| Entities | `ForgeRegistries.ENTITY_TYPES` | Any custom horror entity |
| Sounds | `ForgeRegistries.SOUND_EVENTS` | Ambient stings, music, whispers |
| Particles | `ForgeRegistries.PARTICLE_TYPES` | Smoke, ash, rose petals |
| Features | `Registries.FEATURE` | Custom worldgen features |
| Chunk generators | `Registries.CHUNK_GENERATOR` | Codec registration for a custom generator |
| Biome sources | `Registries.BIOME_SOURCE` | Only if a custom source is needed |
| Creative tabs | `Registries.CREATIVE_MODE_TAB` | Optional; a dedicated tab is nice for testing |

Forge 1.20.1 still uses `RegistryObject`. Do not copy NeoForge 1.21 examples that use `DeferredHolder`; they will not compile here.

## Data versus assets

| Folder | Consumed by | Contains |
| --- | --- | --- |
| `assets/theother/` | Client resource manager | `lang/`, `models/`, `textures/`, `blockstates/`, `sounds.json`, `particles/` |
| `data/theother/` | Server data pack | `dimension/`, `dimension_type/`, `worldgen/`, `loot_tables/`, `tags/`, `recipes/`, `advancements/` |
| `data/minecraft/` | Vanilla namespace overrides | Only when deliberately overriding vanilla data; risky |
| `src/main/templates/` | `generateModMetadata` | `META-INF/mods.toml`, `pack.mcmeta` with `${...}` placeholders |

The `data/` path is **singular for registries and plural for some legacy folders**. In 1.20.1:

- `data/theother/dimension/the_other.json`
- `data/theother/dimension_type/the_other.json`
- `data/theother/worldgen/biome/ashen_forest.json`
- `data/theother/worldgen/configured_feature/...`
- `data/theother/worldgen/placed_feature/...`
- `data/theother/worldgen/structure/...`
- `data/theother/worldgen/structure_set/...`
- `data/theother/worldgen/template_pool/...`
- `data/theother/structures/house.nbt` (plural `structures` in 1.20.1)
- `data/theother/loot_tables/chests/rose_chest.json`
- `data/theother/tags/worldgen/biome/has_structure/rose_podium.json`

When a path is wrong, the game usually logs a missing registry entry and falls back or crashes. Read the log; do not guess twice.

## Networking

The one-life rule and the darkening cinematic both need server→client messages. Forge 1.20.1 uses `SimpleChannel`.

```java
public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TheOther.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        CHANNEL.registerMessage(0, DarkenPayload.class,
                DarkenPayload::encode,
                DarkenPayload::decode,
                DarkenPayload::handle);
    }
}
```

The 1.20.1 handler signature is:

```java
public static void handle(DarkenPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
    NetworkEvent.Context context = contextSupplier.get();
    context.enqueueWork(() -> {
        // client thread work
    });
    context.setPacketHandled(true);
}
```

Rules:

- `enqueueWork` puts the work on the correct thread. Do not touch the world from the network thread.
- Always call `setPacketHandled(true)`.
- Send with `CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload)` on the server.
- Client→server packets must validate the sender and the dimension server-side. Never trust the client for progression.
- 1.20.2+ replaced this API with `CustomPacketPayload`; do not copy newer tutorials.

## Suggested package plan

```text
theother/
├── TheOther.java
├── client/
│   ├── TheOtherClient.java
│   ├── TheOtherTitleScreen.java
│   ├── TheOtherDimensionEffects.java
│   └── DarkeningOverlay.java
├── common/
│   ├── ModConfig.java
│   ├── ModEvents.java
│   └── ModNetwork.java
├── registry/
│   ├── ModItems.java
│   ├── ModBlocks.java
│   ├── ModBlockEntities.java
│   ├── ModSounds.java
│   ├── ModFeatures.java
│   └── ModChunkGenerators.java
├── world/
│   ├── ModDimensions.java
│   ├── ModBiomes.java
│   ├── ModStructures.java
│   └── ModWorldgen.java
├── state/
│   ├── TheOtherState.java
│   ├── ProgressionPhase.java
│   └── LifeLock.java
└── util/
    └── ResourceLocations.java
```

This is a guide, not a rule. Group by feature when a feature spans many classes; group by function when classes are shared. The official Forge structuring guide accepts both.

## Threading and lifecycle rules

- The mod constructor and registry events run before the world exists. Do not query a `ServerLevel`.
- `ServerStartedEvent` is the first safe point for world-level work. It fires once per server start.
- `ServerStoppingEvent` is where you flush non-`SavedData` state.
- `TickEvent.ServerTickEvent` runs 20 times per second on the server thread. Keep work bounded; do not scan every block every tick.
- `ClientTickEvent` runs on the client thread. Do not touch server state.
- Use `server.execute(...)` to move work onto the server thread from another thread.
- `SavedData` is saved with the world; a plain file in the instance is not. Choose deliberately (see [One life](06-one-life.md)).

## Code conventions for this project

- Java 17: records, switch expressions, and `var` are available, but keep gameplay code readable.
- No static state that belongs to a world. A dedicated server can host multiple worlds over its lifetime.
- Use `ResourceLocation` constants in one place.
- Prefer `Component.translatable` over `Component.literal` for anything a player sees.
- Keep client and server code in separate packages.
- Log at `info` for lifecycle milestones and `debug` for per-tick or per-block details.

## Verification

After the first real feature is added:

1. Push to `main`.
2. Open the **Actions** tab and confirm the `Build` workflow is green.
3. Download the `theother-jar` artifact.
4. Download the CI artifact and launch it in a clean Forge 1.20.1 instance. In this project, the rule is CI-only; do not build locally.
5. Confirm the log contains `The Other is loading` and no missing registry errors.

The next guide implements the first visible feature: [the title screen](02-title-screen.md).
