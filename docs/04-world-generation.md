# World generation: superflat start, biomes, features, and structures

## The pipeline in one page

Minecraft 1.20.1 generates a chunk in stages. Each stage can read the output of the previous one:

```text
STRUCTURE_STARTS      choose structure starts
STRUCTURE_REFERENCES  link structure pieces
BIOMES                resolve biomes per quart
NOISE                 fill the terrain shape
SURFACE               apply surface rules (grass, sand, deepslate)
CARVERS               caves and canyons
FEATURES              decorate: ores, trees, lakes, wheat, structures
LIGHT                 compute light
SPAWN                 spawn initial mobs
FULL                  chunk is playable
```

A `ChunkGenerator` owns the noise, surface, and carving stages. JSON owns biomes, features, structures, and loot. A structure is placed during `FEATURES`; a feature is placed during its `GenerationStep.Decoration` list.

## Folder map for this mod

```text
src/main/resources/data/theother/
├── dimension/
│   ├── peaceful.json
│   ├── forest.json
│   └── the_other.json
├── dimension_type/
│   ├── peaceful.json
│   ├── forest.json
│   └── the_other.json
├── worldgen/
│   ├── biome/
│   │   ├── golden_fields.json
│   │   ├── ashen_forest.json
│   │   └── the_other_void.json
│   ├── configured_feature/
│   ├── placed_feature/
│   ├── noise_settings/
│   ├── structure/
│   ├── structure_set/
│   └── template_pool/
├── structures/              <-- plural in 1.20.1; NBT templates
│   ├── house.nbt
│   └── rose_podium.nbt
├── loot_tables/
│   └── chests/rose_chest.json
└── tags/worldgen/biome/
    └── has_structure/rose_podium.json
```

The singular/plural mistakes that cost the most time:

- `dimension/`, `dimension_type/`, `worldgen/structure/`, `worldgen/structure_set/`, `worldgen/template_pool/` are **singular**.
- `structures/`, `loot_tables/`, `tags/`, `recipes/`, `advancements/` are **plural**.
- The NBT template id `theother:house` resolves to `data/theother/structures/house.nbt`.

## Starting world: three ways to ship it

### Option A: a prebuilt world save (recommended for the modpack)

Build the peaceful world once, place the house and basement, save it, and ship the world folder in the modpack. This is the most controllable option: the terrain, house, chest, sign, and spawn point are exactly what you authored, and no worldgen code can drift.

Tradeoff: it is a modpack asset, not a mod feature. A player who installs only the mod gets a normal world. That is acceptable if the modpack is the product.

### Option B: a custom world preset

A `world_preset` adds a selectable preset to the world creation screen. The player must choose it, or the modpack can pre-select it. The preset is a map of `LevelStem`s:

`data/theother/worldgen/world_preset/peaceful.json`:

```json
{
  "dimensions": {
    "minecraft:overworld": {
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
    },
    "minecraft:the_nether": {
      "type": "minecraft:the_nether",
      "generator": {
        "type": "minecraft:noise",
        "biome_source": { "type": "minecraft:multi_noise", "preset": "minecraft:nether" },
        "settings": "minecraft:nether"
      }
    },
    "minecraft:the_end": {
      "type": "minecraft:the_end",
      "generator": {
        "type": "minecraft:noise",
        "biome_source": { "type": "minecraft:the_end" },
        "settings": "minecraft:end"
      }
    }
  }
}
```

If you define only the overworld, the other dimensions may be absent. Include all three stems unless you deliberately want a world without a Nether or End.

Tradeoff: the player can pick a different preset. If the story requires the flat world, use Option A or C.

### Option C: keep the vanilla overworld and teleport on first join

This is the most robust standalone-mod option:

1. Let the player create any world.
2. On the first join, teleport them to `theother:peaceful`.
3. Store a `firstJoin` flag in `SavedData` so it happens once.

The downside is that the player briefly sees the vanilla world and may be confused. Gate it behind a config or a "start the story" button if the mod is not shipped as a curated pack.

Do **not** overwrite `data/minecraft/dimension/overworld.json` from a mod. It changes every world, conflicts with other mods, and is hard to undo. Use a custom dimension instead.

## Biomes

A minimal 1.20.1 biome:

`src/main/resources/data/theother/worldgen/biome/golden_fields.json`:

```json
{
  "temperature": 0.8,
  "downfall": 0.4,
  "has_precipitation": true,
  "effects": {
    "sky_color": 7907327,
    "fog_color": 12638463,
    "water_color": 4159204,
    "water_fog_color": 329011,
    "grass_color": 9551193,
    "foliage_color": 7842607
  },
  "spawners": {
    "ambient": [],
    "axolotls": [],
    "creature": [],
    "misc": [],
    "monster": [],
    "underground_water_creature": [],
    "water_ambient": [],
    "water_creature": []
  },
  "spawn_costs": {},
  "carvers": {},
  "features": [
    [],
    [],
    [],
    [],
    [],
    [],
    [],
    [],
    [],
    [],
    []
  ]
}
```

- `has_precipitation` is the 1.20.1 field. Older tutorials use `precipitation`; that will fail codec validation.
- `spawners` has one list per mob category. Empty lists mean no natural spawns. This is how the peaceful world stays peaceful even if the dimension type's light test would allow monsters.
- `features` has one list per `GenerationStep.Decoration` step. Vanilla 1.20.1 biomes use 11 lists; copy that shape and insert your placed features at the matching index. In practice, trees, grass, and flowers go in the `VEGETAL_DECORATION` list (index 9 in vanilla files); lakes go earlier.
- `carvers` and `spawn_costs` can be empty objects.
- Colors are decimal ARGB/RGB values. For the forest, lower the sky and fog colors toward gray-green; for The Other, near black.

For the forest, add a custom mood sound:

```json
"mood_sound": {
  "block_search_extent": 8,
  "offset": 2.0,
  "sound": "theother:ambient.forest_whisper",
  "tick_delay": 6000
}
```

The sound must exist in `sounds.json` and be registered as a `SoundEvent` (see [Assets and data](08-assets-and-data.md)).

## Features

A configured feature describes **what** to place; a placed feature describes **where** and **how often**.

`data/theother/worldgen/configured_feature/ashen_tree.json`:

```json
{
  "type": "minecraft:tree",
  "config": {
    "trunk_provider": {
      "type": "minecraft:simple_state_provider",
      "state": { "Name": "minecraft:dark_oak_log", "Properties": { "axis": "y" } }
    },
    "foliage_provider": {
      "type": "minecraft:simple_state_provider",
      "state": { "Name": "minecraft:dark_oak_leaves", "Properties": { "distance": "7", "persistent": "false", "waterlogged": "false" } }
    },
    "trunk_placer": { "type": "minecraft:straight_trunk_placer", "base_height": 5, "height_rand_a": 2, "height_rand_b": 1 },
    "foliage_placer": { "type": "minecraft:blob_foliage_placer", "height": 3, "offset": 0, "radius": 2 },
    "decorators": [],
    "minimum_size": { "type": "minecraft:two_layers_feature_size" },
    "ignore_vines": true,
    "force_dirt": false,
    "dirt_provider": { "type": "minecraft:simple_state_provider", "state": { "Name": "minecraft:dirt" } }
  }
}
```

`data/theother/worldgen/placed_feature/ashen_trees.json`:

```json
{
  "feature": "theother:ashen_tree",
  "placement": [
    { "type": "minecraft:count", "count": 3 },
    { "type": "minecraft:in_square" },
    { "type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG" },
    { "type": "minecraft:biome" }
  ]
}
```

Then add `"theother:ashen_trees"` to the biome's vegetation list.

For the wheat fields in the peaceful world, do not use a vanilla `random_patch`: wheat needs farmland and water. Build the fields in the house/structure NBT, or write a custom `Feature` that places farmland, water, and wheat in a rectangle. The NBT route is simpler and matches the "authored diorama" feel.

## Structures

### The fixed house: NBT template

Build the house in a creative world, select it with a structure block, and save it as `house.nbt`. Place it at a fixed position on server start. This is more deterministic than a jigsaw and easier to iterate than a custom feature.

NBT contents worth checking:

- A chest with `LootTable: "theother:chests/rose_chest"` and `id: "minecraft:chest"`.
- A sign with `front_text.messages` (a list of JSON strings), `front_text.color`, and `is_waxed`.
- A carpet over a trapdoor: `minecraft:white_carpet` on top of `minecraft:oak_trapdoor` with `half=top`, `open=false`.
- A ladder column with `facing` and `waterlogged` on every block.
- The basement room, chest, sign, and enough air around the ladder.

A 1.20.1 sign NBT fragment:

```snbt
{
  id: "minecraft:sign",
  front_text: {
    messages: ['{"text":"do not open, or else"}', '""', '""', '""'],
    color: "black",
    has_glowing_text: 0b
  },
  is_waxed: 0b
}
```

A chest fragment:

```snbt
{
  id: "minecraft:chest",
  LootTable: "theother:chests/rose_chest"
}
```

Loot table `data/theother/loot_tables/chests/rose_chest.json`:

```json
{
  "type": "minecraft:chest",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        { "type": "minecraft:item", "name": "theother:enchanted_rose" }
      ]
    }
  ]
}
```

The item must be registered (`ModItems.ENCHANTED_ROSE`) and have a model at `assets/theother/models/item/enchanted_rose.json` plus a texture.

### Runtime placement

```java
@Mod.EventBusSubscriber(modid = TheOther.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorldSetup {
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(ResourceKey.create(
                Registries.DIMENSION,
                new ResourceLocation(TheOther.MODID, "peaceful")
        ));
        if (level == null) {
            return;
        }

        TheOtherState state = TheOtherState.get(level);
        if (state.isHousePlaced()) {
            return;
        }

        level.getStructureManager().get(new ResourceLocation(TheOther.MODID, "house")).ifPresent(template -> {
            BlockPos origin = new BlockPos(8, 4, 8);
            StructurePlaceSettings settings = new StructurePlaceSettings()
                    .setIgnoreEntities(false)
                    .setMirror(Mirror.NONE)
                    .setRotation(Rotation.NONE)
                    .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);

            template.placeInWorld(level, origin, origin, settings, level.getRandom(), 2);
            state.setHousePlaced(true);
            state.setDirty();
        });
    }
}
```

Notes:

- `StructureTemplateManager#get` resolves `data/theother/structures/house.nbt`.
- `placeInWorld(level, pos, pivot, settings, random, flags)` takes the placement position and the rotation pivot. Pass the same `BlockPos` for both unless you rotate around another point.
- The `flags` value `2` is the standard `Block.UPDATE_CLIENTS`; use `3` if you also need neighbour updates (`UPDATE_NEIGHBORS | UPDATE_CLIENTS`). Be deliberate: a large structure with `3` can cause cascading updates.
- `BlockIgnoreProcessor.STRUCTURE_BLOCK` removes structure blocks from the placed result. Add `BlockIgnoreProcessor.STRUCTURE_AND_AIR` only if you want air blocks in the template to erase terrain.
- Place the house **after** the level exists. `ServerStartedEvent` is safe; the mod constructor is not.
- Set the world spawn next to the house:
  `level.setDefaultSpawnPos(origin.offset(0, 1, 0), 180.0F);`
  Check the mapped method name; `ServerLevel` exposes a default-spawn setter in 1.20.1.
- Guard with `SavedData` so a restart does not duplicate the house. The flag is saved with the world.

### The podium: jigsaw structure

A jigsaw is the right tool for a structure that should be discovered in the forest and placed on terrain.

`data/theother/worldgen/structure/rose_podium.json`:

```json
{
  "type": "minecraft:jigsaw",
  "biomes": "#theother:has_structure/rose_podium",
  "step": "surface_structures",
  "spawn_overrides": {},
  "terrain_adaptation": "beard_thin",
  "start_pool": "theother:rose_podium/start_pool",
  "size": 1,
  "start_height": { "absolute": 0 },
  "project_start_to_heightmap": "WORLD_SURFACE_WG",
  "max_distance_from_center": 80,
  "use_expansion_hack": false
}
```

`data/theother/worldgen/structure_set/rose_podium.json`:

```json
{
  "structures": [
    { "structure": "theother:rose_podium", "weight": 1 }
  ],
  "placement": {
    "type": "minecraft:random_spread",
    "spacing": 32,
    "separation": 8,
    "salt": 123456789
  }
}
```

`data/theother/worldgen/template_pool/rose_podium/start_pool.json`:

```json
{
  "name": "theother:rose_podium/start_pool",
  "fallback": "minecraft:empty",
  "elements": [
    {
      "weight": 1,
      "element": {
        "element_type": "minecraft:single_pool_element",
        "location": "theother:rose_podium",
        "processors": "minecraft:empty",
        "projection": "rigid"
      }
    }
  ]
}
```

`data/theother/tags/worldgen/biome/has_structure/rose_podium.json`:

```json
{
  "values": [
    "theother:ashen_forest"
  ]
}
```

- `start_height` is an `HeightProvider`; `{"absolute": 0}` is a valid constant.
- `project_start_to_heightmap` makes the structure follow the surface. Use `WORLD_SURFACE_WG` (worldgen surface), not `WORLD_SURFACE` (post-generation heightmap).
- `terrain_adaptation` can flatten or bury the area. `beard_thin` blends the base; `none` leaves the terrain untouched. A podium on a flat clearing probably wants `none` plus a deliberately flat NBT base.
- `max_distance_from_center` limits the jigsaw expansion. A single-element pool only needs a small value, but vanilla uses 80; keep 80 for safety.
- `size` is the maximum jigsaw depth. `1` means only the start pool. If you add connecting pieces, raise it.
- `structure_set` spacing is in chunks. `spacing: 32`, `separation: 8` means at most one podium per 32×32 chunk region. Do not use spacing below 8 for a large structure.

### The mud road and waterways

A road that crosses the forest is awkward as one NBT. Options:

1. **Jigsaw road pieces** — a start pool of straight/corner/t-junction pieces with `terrain_adaptation: none`. This is how villages work. It is the most flexible and the most work.
2. **A custom feature** — place a line of mud blocks along a direction from the podium. Easier, but it ignores terrain and can float.
3. **A prebuilt forest region in the modpack save** — best for a authored cinematic route. The forest dimension can be small and hand-built.

The plan says "in the middle of a mud road" and "leads to a wide circular podium". A short, authored road in a prebuilt save is the most reliable way to hit the composition. Use jigsaw only if the forest is procedural.

### Trees, grass, and water

- Trees: a tree configured feature + placed feature in the forest biome's vegetation list.
- Grass: `minecraft:patch_grass_forest` or a custom `random_patch` of `minecraft:short_grass`.
- Water: `minecraft:lake_water` as a placed feature, or a custom feature that carves a channel. For a flat forest, `lakes: true` in the flat settings is the simplest.
- Wheat: farmland + water + wheat in an authored NBT, or a custom feature. Vanilla `random_patch` of wheat will fail on grass because wheat needs farmland.

## Testing worldgen

```mcfunction
# Place a structure template directly (fast iteration)
place template theother:house 8 4 8

# Place a jigsaw structure at the player's position
place structure theother:rose_podium

# Find the nearest generated podium
locate structure theother:rose_podium

# Inspect a biome
execute in theother:forest run locate biome theother:ashen_forest
```

For dimension changes, restart the world. For structure JSON and loot tables, `/reload` is often enough, but a restart is the reliable check.

What success looks like:

- The house appears once, at the intended origin, with the chest, sign, carpet, trapdoor, and ladder intact.
- The chest contains the rose; the loot table resolves, not a missing-table error.
- The forest has trees, grass, water, and the podium in a sensible place.
- The podium is not buried and does not float.
- The console has no `Unable to load structure`, `Unknown biome`, or codec errors.

## Performance notes

- A large NBT placed at server start is a one-time cost. A large NBT placed per chunk is a permanent cost; avoid it.
- Jigsaw structures generate during `STRUCTURE_STARTS` and `FEATURES`. Keep `spacing` generous and `size` small.
- Every placed feature runs for every chunk in the biome. A tree feature with `count: 10` is ten attempts per chunk; multiply by the biome area.
- `terrain_adaptation: beard_box` on a huge structure can reshape a lot of terrain and cause cascading block updates. Prefer `none` or `beard_thin` for the podium.
- Test with the same performance mods the pack will ship. Worldgen bugs often appear only under Embeddium/Canary.

## Pitfalls

| Symptom | Cause | Fix |
| --- | --- | --- |
| `Unable to load structure theother:house` | Wrong folder (`structure` vs `structures`) | Use `data/theother/structures/house.nbt` |
| House places but the chest is empty | Loot table path or item id is wrong | `data/theother/loot_tables/chests/rose_chest.json`, item registered |
| Sign is blank | `front_text.messages` is not a list of JSON strings | Use `'{"text":"..."}'` entries |
| Podium is buried | `terrain_adaptation` or `start_height` | Try `none`, and a heightmap projection |
| Structure never generates | Biome tag missing or wrong | `data/theother/tags/worldgen/biome/has_structure/rose_podium.json` |
| `features` codec error | Wrong number of lists or wrong feature id | Copy a vanilla 1.20.1 biome shape and validate ids |
| Trees float | Heightmap is `WORLD_SURFACE` instead of `WORLD_SURFACE_WG` | Use the worldgen heightmap |
| House duplicates on restart | No `SavedData` guard | Set and save a `housePlaced` flag |
| World lags on first join | Huge NBT/feature work on the server thread | Move setup to `ServerStartedEvent`, keep NBT reasonable |

## Sources

- Vanilla 1.20.1 worldgen data: <https://github.com/misode/mcmeta/tree/1.20.1-data/data/minecraft/worldgen>
- Minecraft Wiki, world generation: <https://minecraft.wiki/w/World_generation>
- Minecraft Wiki, custom structure: <https://minecraft.wiki/w/Structure>
- Misode's generators: <https://misode.github.io/worldgen/>
- Forge 1.20.1 documentation: <https://docs.minecraftforge.net/en/1.20.1/>
