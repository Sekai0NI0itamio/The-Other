# Custom dimensions and worlds (1.20.1 Forge)

## Mental model

In 1.20.1, a dimension is **data**, not a class you register. The game builds it from two JSON files:

```text
data/theother/dimension_type/the_other.json   -> what the dimension feels like
data/theother/dimension/the_other.json        -> how the dimension is generated
```

The id is the path under the folder:

- `data/theother/dimension/the_other.json` → dimension `theother:the_other`
- `data/theother/dimension_type/the_other.json` → dimension type `theother:the_other`

The dimension file's `type` field points at the dimension type id. The dimension type's `effects` field points at a client-side `DimensionSpecialEffects` id.

A `ServerLevel` is created from a `LevelStem`, which pairs the dimension type with a chunk generator. Forge does not need a code registration for a normal datapack dimension. The level exists after the server starts and can be retrieved with:

```java
ServerLevel other = server.getLevel(ResourceKey.create(
        Registries.DIMENSION,
        new ResourceLocation(TheOther.MODID, "the_other")
));
```

`server.getLevel(...)` returns `null` if the dimension is not loaded. Always guard it.

## The three dimensions this mod needs

| Dimension | Role | Generator | Time | Light |
| --- | --- | --- | --- | --- |
| `theother:peaceful` | Starting wheat world, house, basement | Flat or gentle custom terrain | Normal day cycle | Skylight |
| `theother:forest` | Ghostly smoke forest, mud road, podium | Noise or custom terrain | Fixed night (`fixed_time: 18000`) | Dim skylight |
| `theother:the_other` | The black one-life world | Custom generator | Fixed night | No skylight, near-zero ambient |

The plan's "the trap door no longer leads to the house" is a **level transition**, not a terrain mutation. Teleport the player between these dimensions and keep the basement in the peaceful dimension (or copy it into the forest if the player must look back).

## Dimension type JSON

### Peaceful

`src/main/resources/data/theother/dimension_type/peaceful.json`:

```json
{
  "ambient_light": 0.0,
  "bed_works": true,
  "coordinate_scale": 1.0,
  "effects": "minecraft:overworld",
  "has_ceiling": false,
  "has_raids": false,
  "has_skylight": true,
  "height": 384,
  "infiniburn": "#minecraft:infiniburn_overworld",
  "logical_height": 384,
  "min_y": -64,
  "monster_spawn_block_light_limit": 0,
  "monster_spawn_light_level": 0,
  "natural": true,
  "piglin_safe": false,
  "respawn_anchor_works": false,
  "ultrawarm": false
}
```

`monster_spawn_light_level` only controls the light test. To guarantee "no mobs spawn there", also give every biome an empty `spawners` object (see [World generation](04-world-generation.md)). A flat world with a normal plains biome can still spawn passive animals unless the spawn list is emptied.

### Forest

`src/main/resources/data/theother/dimension_type/forest.json`:

```json
{
  "ambient_light": 0.05,
  "bed_works": false,
  "coordinate_scale": 1.0,
  "effects": "theother:forest",
  "fixed_time": 18000,
  "has_ceiling": false,
  "has_raids": false,
  "has_skylight": true,
  "height": 384,
  "infiniburn": "#minecraft:infiniburn_overworld",
  "logical_height": 384,
  "min_y": -64,
  "monster_spawn_block_light_limit": 0,
  "monster_spawn_light_level": 0,
  "natural": false,
  "piglin_safe": false,
  "respawn_anchor_works": false,
  "ultrawarm": false
}
```

`fixed_time` keeps the sky at night. **Game time still ticks**: crops grow, furnaces burn, and `dayTime` is pinned. That matches "forever night, time does pass" better than freezing the entire level. If you want a slow cycle instead, omit `fixed_time` and force the time in a server tick handler; that is more code and more risk of fighting vanilla sleep logic.

`bed_works: false` prevents a bed from becoming a respawn point in the forest. If the player should be able to set a spawn there, set it to `true`; decide deliberately.

### The Other

`src/main/resources/data/theother/dimension_type/the_other.json`:

```json
{
  "ambient_light": 0.0,
  "bed_works": false,
  "coordinate_scale": 1.0,
  "effects": "theother:the_other",
  "fixed_time": 18000,
  "has_ceiling": false,
  "has_raids": false,
  "has_skylight": false,
  "height": 384,
  "infiniburn": "#minecraft:infiniburn_overworld",
  "logical_height": 384,
  "min_y": -64,
  "monster_spawn_block_light_limit": 0,
  "monster_spawn_light_level": 0,
  "natural": false,
  "piglin_safe": false,
  "respawn_anchor_works": false,
  "ultrawarm": false
}
```

`has_skylight: false` removes the sky light contribution. Combined with a near-black fog and a custom effects class, the world reads as black even before the cinematic overlay. `height` and `min_y` must match the generator's height; a mismatch causes chunk errors or invisible terrain.

## Dimension JSON

### Flat starting world

`src/main/resources/data/theother/dimension/peaceful.json`:

```json
{
  "type": "theother:peaceful",
  "generator": {
    "type": "minecraft:flat",
    "settings": {
      "biome": "theother:golden_fields",
      "lakes": false,
      "features": true,
      "layers": [
        { "block": "minecraft:bedrock", "height": 1 },
        { "block": "minecraft:dirt", "height": 3 },
        { "block": "minecraft:grass_block", "height": 1 }
      ],
      "structure_overrides": []
    }
  }
}
```

This gives a clean superflat canvas. "Hills of wheat, golden hills" is not a flat layer; add it with a custom generator, a feature, or a structure. The simplest credible version is a flat base plus placed wheat fields and hay-bale mounds around the house.

### Noise terrain

For a natural forest, copy the shape of the vanilla overworld and change the noise settings and biome source:

```json
{
  "type": "theother:forest",
  "generator": {
    "type": "minecraft:noise",
    "settings": "theother:forest_noise",
    "biome_source": {
      "type": "minecraft:multi_noise",
      "preset": "theother:forest"
    }
  }
}
```

`multi_noise` presets live at `data/theother/worldgen/multi_noise_biome_source_parameter_list/forest.json`. For a single biome, use the simpler `minecraft:fixed` source:

```json
"biome_source": {
  "type": "minecraft:fixed",
  "biome": "theother:ashen_forest"
}
```

A custom noise settings file is large. Copy `data/minecraft/worldgen/noise_settings/overworld.json` from the 1.20.1 data pack, then tune `sea_level`, `default_block`, `default_fluid`, and the `noise_router`. Build it with a generator such as <https://misode.github.io/worldgen/> and validate it by loading the world; a malformed router fails at startup with a codec error.

### The Other: a custom generator

For a deliberately empty or shaped black world, use a custom `ChunkGenerator`. The JSON:

```json
{
  "type": "theother:the_other",
  "generator": {
    "type": "theother:void",
    "biome_source": {
      "type": "minecraft:fixed",
      "biome": "theother:the_other_void"
    }
  }
}
```

`generator.type` is the id of the **codec** registered in `Registries.CHUNK_GENERATOR`, not the dimension id. Register it under `theother:void`.

## Custom chunk generator

```java
package io.github.sekai0ni0itamio.theother.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class VoidChunkGenerator extends ChunkGenerator {
    public static final Codec<VoidChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, VoidChunkGenerator::new));

    public VoidChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void fillFromNoise(Executor executor, Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        return new NoiseColumn(level.getMinBuildHeight(), new BlockState[0]);
    }
}
```

Notes:

- `getGenDepth()` must match the dimension's `height` (384 here). A mismatch produces broken chunk sections.
- `buildSurface` and `fillFromNoise` are abstract in 1.20.1. Implement them even if empty.
- `getBaseHeight` is used by heightmaps and structure placement. Returning the minimum height means "nothing solid".
- `getBaseColumn` is used by features and carvers. An empty column is correct for a void world.
- To place a floor, override `fillFromNoise` to write blocks, or use a `FlatLevelSource` instead. Do not write blocks in `getBaseHeight`; it is a query.
- `applyCarvers`, `spawnOriginalMobs`, and `createStructures` have defaults. Override only when needed.

Register the codec:

```java
public final class ModChunkGenerators {
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, TheOther.MODID);

    public static final RegistryObject<Codec<? extends ChunkGenerator>> VOID =
            CHUNK_GENERATORS.register("void", () -> VoidChunkGenerator.CODEC);

    public static void register(IEventBus modBus) {
        CHUNK_GENERATORS.register(modBus);
    }
}
```

Call `ModChunkGenerators.register(modEventBus)` from the mod constructor.

### Custom biome source

Only needed if the dimension's biomes depend on coordinates. For a single biome, use `minecraft:fixed` and skip this. For a custom source, register a `Codec<? extends BiomeSource>` in `Registries.BIOME_SOURCE` the same way, then set `"biome_source": {"type": "theother:my_source", ...}`.

## Teleporting a player

```java
public static void sendTo(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos) {
    MinecraftServer server = player.getServer();
    if (server == null) {
        return;
    }
    ServerLevel target = server.getLevel(dimension);
    if (target == null) {
        TheOther.LOGGER.warn("Dimension {} is not loaded", dimension.location());
        return;
    }
    player.changeDimension(target, new ITeleporter() {
        @Override
        public Entity placeEntity(Entity entity, ServerLevel currentLevel, ServerLevel destination, float yaw,
                                  Function<Boolean, Entity> repositionEntity) {
            Entity placed = repositionEntity.apply(false);
            placed.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, entity.getXRot());
            return placed;
        }
    });
}
```

- `changeDimension` handles the client-side dimension switch, chunk loading, and entity re-creation.
- `repositionEntity.apply(false)` reuses the entity; `true` forces a new one. For a player, `false` is correct.
- `moveTo` sets position and rotation. Set the position **before** the player ticks, or they may fall through unloaded chunks.
- `player.teleportTo(target, x, y, z, yaw, pitch)` is also available in 1.20.1, but the `ITeleporter` route gives explicit control over the placement callback.
- Do not teleport during the `LivingDeathEvent`; the player is in a dying state. Schedule the move on the next server tick.

## Client-side dimension effects

The `effects` field names a client `DimensionSpecialEffects`. Register it on the **mod bus**, on the client:

```java
@Mod.EventBusSubscriber(modid = TheOther.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientDimensions {
    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(new ResourceLocation(TheOther.MODID, "the_other"), new TheOtherDimensionEffects());
    }
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

The constructor arguments are:

1. `cloudsHeight` — `Float.NaN` disables clouds.
2. `alternateSkyColor` — whether the sky color uses the alternate path.
3. `skyType` — `NONE`, `NORMAL`, or `END`.
4. `brightenLighting` — force the bright lightmap.
5. `darkened` — darken the ambient light.

In 1.20.1 `DimensionSpecialEffects` only exposes fog, sunrise, cloud height, and lightmap controls. It does **not** expose `renderSky` or `renderClouds`; custom sky geometry needs a render event or a mixin into `LevelRenderer`. Do not copy 1.21 examples that override sky rendering here.

The `effects` id in `dimension_type/the_other.json` must be `theother:the_other` to match `event.register(...)`. If it is `minecraft:the_end`, the custom class is never used.

## Portals, respawn, and coordinates

- **Portal blocks** are a block + a teleporter + frame validation. For a scripted story, a custom trapdoor or a `PlayerInteractEvent` is simpler and more reliable than a Nether-style portal.
- **Coordinate scale** is `coordinate_scale` in the dimension type. Keep it `1.0` unless you deliberately want Nether-style 8:1 mapping.
- **Respawn**: with `bed_works: false` and `respawn_anchor_works: false`, a dead player respawns at the overworld spawn (or their global spawn). That is useful for The Other: death must not silently respawn the player inside the locked world.
- **World spawn**: set it once when the starting world is created. `ServerLevel#setDefaultSpawnPos(BlockPos, float)` changes where new players spawn; do it from `ServerStartedEvent`, not the mod constructor.

## Adding dimensions to an existing world

Dimension JSON is loaded at **server startup**. `/reload` reloads recipes, loot, tags, and some worldgen data, but it does not create a new `ServerLevel`. After adding or renaming a dimension:

1. Stop the server/world.
2. Start it again.
3. Run `/execute in theother:the_other run tp @s 0 100 0` to verify the level exists.

If the dimension is missing, the log reports an unknown dimension key or a codec error. Read the first error, not the last stack trace.

## Testing

```mcfunction
# Check that the level exists and is reachable
execute in theother:peaceful run tp @s 0 100 0
execute in theother:forest run tp @s 0 100 0
execute in theother:the_other run tp @s 0 100 0

# Keep chunks loaded while inspecting terrain
forceload add 0 0 512 512

# Check time and weather
time query daytime
weather clear
```

Visual checks:

- The peaceful world is flat, bright, and mob-free.
- The forest is dark at all times, but crops still grow.
- The Other has no sky light; the fog is black; the horizon disappears.
- Beds do not explode in the forest if `bed_works: false` (they are simply unusable).
- Returning from The Other to the forest does not leave the player falling through unloaded chunks.

## Pitfalls

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `Unknown dimension type` | `type` points at a missing id | Match `data/<ns>/dimension_type/<path>.json` |
| Dimension exists but is empty | Generator codec not registered, or biome source invalid | Check the codec id and the startup log |
| Player falls forever | Teleport Y is below the terrain or chunks are unloaded | Use a safe Y and `changeDimension`, not a raw position set |
| World is bright despite `has_skylight: false` | `ambient_light` too high, or `effects` still the overworld | Set `ambient_light` low and register custom effects |
| Beds work in The Other | `bed_works: true` | Set it to `false` |
| `getGenDepth` mismatch | Dimension `height` differs from the generator | Keep both at 384, or make the generator read the height |
| Custom fog never appears | Effects registered under a different id | Match `effects` and `event.register` exactly |
| `/reload` does not add the dimension | Levels are created at startup | Restart the server |

## Sources

- Vanilla 1.20.1 dimension data: <https://github.com/misode/mcmeta/tree/1.20.1-data/data/minecraft/dimension>
- Vanilla 1.20.1 dimension types: <https://github.com/misode/mcmeta/tree/1.20.1-data/data/minecraft/dimension_type>
- Minecraft Wiki, custom dimension: <https://minecraft.wiki/w/Custom_dimension>
- Minecraft Wiki, dimension type: <https://minecraft.wiki/w/Dimension_type>
- Forge 1.20.1 `RegisterDimensionSpecialEffectsEvent`: <https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/client/event/RegisterDimensionSpecialEffectsEvent.java>
- Misode's worldgen generator: <https://misode.github.io/worldgen/>
