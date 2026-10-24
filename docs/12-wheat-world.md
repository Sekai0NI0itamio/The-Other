# Wheat world

This guide covers the first playable slice of The Other:

- A title screen with only **Play** and **Settings**.
- **Play** creates or loads the `TheOther` world, an infinite wheat field with subtle hills.
- Natural mob spawning is blocked in every world.

House copying is **not** part of this mod. Use the separate [HouseCopy](https://github.com/Sekai0NI0itamio/HouseCopy) mod for that.

## Title screen

`TheOtherTitleScreen` has exactly two controls:

| Control | Action |
| --- | --- |
| **Play** | `TheOtherWorld.play(this)` |
| **Settings** | Opens the vanilla `OptionsScreen` |

There is no multiplayer button, no quit button, and no hidden world-list shortcut. The window close control still exits the game.

## Play: create or load the wheat world

`TheOtherWorld` uses the vanilla `WorldOpenFlows` API:

```java
if (minecraft.getLevelSource().levelExists("TheOther")) {
    minecraft.createWorldOpenFlows().loadLevel(parent, "TheOther");
    return;
}
```

If the world does not exist, it creates it with a fixed configuration:

| Setting | Value |
| --- | --- |
| Folder | `TheOther` |
| Display name | `The Other` |
| Seed | `0` |
| Gamemode | Survival |
| Difficulty | Peaceful |
| Cheats | Enabled |
| Structures | Disabled |
| Overworld generator | `WheatChunkGenerator` |
| Overworld biome | `theother:wheat_fields` |

The Nether and End are still generated from the normal preset, but the player starts in the overworld wheat field. They are not part of the current slice.

### Why the custom biome matters

The wheat biome is `data/theother/worldgen/biome/wheat_fields.json`. It has:

- Empty `spawners` for every mob category.
- Empty `features` lists, so no grass, flowers, trees, or lakes generate on top of the wheat.
- Vanilla-like sky and fog colors.

Without the custom biome, the plains biome would decorate the farmland with trees and grass, and the result would not be "only the wheat land."

### Why structures are disabled

`WorldOptions(0L, false, false)` disables structure generation. Otherwise villages and other overworld structures could appear in the wheat field.

## The wheat terrain

`WheatChunkGenerator` is a custom `ChunkGenerator` registered as `theother:wheat` in `Registries.CHUNK_GENERATOR`.

### Height

The base surface is Y `64`. The height wave is:

```java
Math.sin(x * 0.025) * 1.5
+ Math.cos(z * 0.021) * 1.5
+ Math.sin((x + z) * 0.009) * 1.0
```

The maximum offset is about ±4 blocks, so the field reads as flat with gentle rolls rather than hills. The wave is deterministic: the same coordinates always produce the same height. The seed does not change it.

### Layers

From the bottom of the world to the surface:

| Y | Block |
| --- | --- |
| `-64` | Bedrock |
| Above `-64` to `surface - 3` | Stone |
| `surface - 3` to `surface - 1` | Dirt |
| `surface` | Farmland (`moisture=7`) |
| `surface + 1` | Wheat (`age=7`) |

The entire field is farmland with wheat, including the spawn area. There is no grass patch and no clearing.

The wheat is fully grown and planted on hydrated farmland. Farmland with a crop does not revert to dirt, so no water channels are needed. The player can harvest it, replant it, or add water later.

### Spawn

`getSpawnHeight(0, 0)` returns `surfaceY(0, 0) + 2`, so the player starts above the wheat and settles onto the farmland. `getBaseHeight` returns the first air block above the surface, so heightmaps and structure placement agree with the terrain.

### What is not generated

- No caves or carvers.
- No ores.
- No trees, grass, flowers, or water.
- No structures.
- No mobs (peaceful difficulty plus empty spawners).

### No natural mob spawns anywhere

The mod enforces a global rule, not just the wheat biome:

- `MobSpawnEvents` listens to `MobSpawnEvent.FinalizeSpawn` and calls `setSpawnCancelled(true)` for every spawn type except `COMMAND`, `SPAWN_EGG`, `BREEDING`, and `MOB_SUMMONED`. Natural, chunk-generation, spawner, structure, patrol, reinforcement, and conversion spawns are blocked.
- `ServerStartedEvent` sets the `doMobSpawning` game rule to `false` on every loaded level.

Spawn eggs, `/summon`, and player-driven breeding still work for testing. If you want absolutely nothing, remove those exceptions from `MobSpawnEvents`.

This is intentional for the first slice. The field is a canvas, not the final level.

## Current limitations

- The wheat world is generated once per world. Changing the generator later does **not** regenerate an existing `TheOther` world. Delete `.minecraft/saves/TheOther` to regenerate it.
- The world seed is fixed, but the terrain function is coordinate-based, so the seed has no visible effect on the wheat.
- The Nether and End are normal dimensions and are reachable only by commands/portals. They are not part of the current slice.
- The one-life rule, the basement, the chest, and the forest are still design/docs only.

## Testing

```mcfunction
/time set day
/gamemode survival
/tp @s 0 70 0
/execute in minecraft:overworld run locate biome theother:wheat_fields
```

Expected:

- The field is wheat in every direction, with gentle height changes.
- No trees, grass, or mobs.
- The spawn is in the wheat, with no grass patch or clearing.
- Hostile mobs do not spawn naturally; spawn eggs and `/summon` still work.

## Build and CI

The project remains **CI-only**. Push to `main`, let the `Build` workflow compile, and download the `theother-jar` artifact. Do not run a local Gradle build.

## Sources

- `WorldOpenFlows` 1.20.1: <https://mappings.dev/1.20.1/net/minecraft/client/gui/screens/worldselection/WorldOpenFlows.html>
- `ChunkGenerator` 1.20.1: <https://mappings.dev/1.20.1/net/minecraft/world/level/chunk/ChunkGenerator.html>
- Forge 1.20.1 documentation: <https://docs.minecraftforge.net/en/1.20.1/>
