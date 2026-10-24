# Wheat world and structure export

This guide covers the first playable slice added after the scaffold:

- A title screen with only **Play** and **Settings**.
- **Play** creates or loads the `TheOther` world, which is currently an infinite wheat field with subtle hills.
- A `/theother export` command that copies a selected box into a `.nbt` structure file for later placement.

The house is not placed yet. The maps and the export tool exist so the house can be chosen, edited in-game, exported, and then added as a structure in a later commit.

## Title screen

`TheOtherTitleScreen` now has exactly two controls:

| Control | Action |
| --- | --- |
| **Play** | `TheOtherWorld.play(this)` |
| **Shift + Play** | Opens the vanilla `SelectWorldScreen` for editing a downloaded map |
| **Settings** | Opens the vanilla `OptionsScreen` |

There is no multiplayer button and no quit button. The window close control still exits the game. Shift+Play is an invisible shortcut, not a third button; it exists so the downloaded maps can still be opened while the menu stays two-button minimal.

The screen keeps the dark red-to-black gradient, the title, the subtitle, and a small "One life." tagline. The tagline is cosmetic; the actual one-life rule is not implemented yet.

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
| Cheats | Enabled (required for the export command) |
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

`WorldOptions(0L, false, false)` disables structure generation. Otherwise villages and other overworld structures could appear in the wheat field. `false` for the second argument is `generateStructures`; the third is the bonus chest.

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

A 9×9 area around `(0, 0)` is cleared for spawn:

- `surface` is grass instead of farmland.
- `surface + 1` is air instead of wheat.

The wheat is fully grown and planted on hydrated farmland. Farmland with a crop does not revert to dirt, so no water channels are needed. The player can harvest it, replant it, or add water later.

### Spawn

`getSpawnHeight(0, 0)` returns `surfaceY(0, 0) + 1`. The player stands on the grass clearing and looks out over the wheat. `getBaseHeight` returns the first air block above the surface, so heightmaps and structure placement agree with the terrain.

### What is not generated

- No caves or carvers.
- No ores.
- No trees, grass, flowers, or water.
- No structures.
- No mobs (peaceful difficulty plus empty spawners).

### No natural mob spawns anywhere

The mod also enforces a global rule, not just the wheat biome:

- `MobSpawnEvents` listens to `MobSpawnEvent.FinalizeSpawn` and calls `setSpawnCancelled(true)` for every spawn type except `COMMAND`, `SPAWN_EGG`, `BREEDING`, and `MOB_SUMMONED`. Natural, chunk-generation, spawner, structure, patrol, reinforcement, and conversion spawns are blocked.
- `ServerStartedEvent` sets the `doMobSpawning` game rule to `false` on every loaded level.

Spawn eggs, `/summon`, and player-driven breeding still work for testing. If you want absolutely nothing, remove those exceptions from `MobSpawnEvents`.

This is intentional for the first slice. The field is a canvas, not the final level.

## The export command

### Syntax

```mcfunction
/theother export <name> <x1> <y1> <z1> <x2> <y2> <z2>
```

Example:

```mcfunction
/theother export wheat_house 120 64 -40 145 84 -15
```

- `<name>` is a word (`a-z`, `0-9`, `_`); it becomes the file name.
- The two positions are opposite corners. The command normalizes them, so order does not matter.
- The selection is inclusive.
- The maximum size is 128 blocks on each axis. The command refuses larger boxes.
- The command requires permission level 2. In the Play-created world, cheats are enabled, so the host has permission.

### What it writes

The command:

1. Loads every chunk touched by the selection.
2. Copies the blocks and block entities with `StructureTemplate.fillFromWorld(...)`. Entities are excluded so the player and nearby mobs cannot end up inside the structure.
3. Saves the structure NBT to:

```text
<game directory>/theother-exports/<name>.nbt
```

On a normal client that is:

```text
.minecraft/theother-exports/<name>.nbt
```

The file is a standard structure NBT, the same format as `data/<namespace>/structures/<name>.nbt`. It can be placed with a structure block or with `StructureTemplate` code.

### How to use it with a downloaded map

1. Unzip the chosen map into `.minecraft/saves/`.
2. Launch the 1.20.1 Forge instance with The Other installed.
3. Open the map, find the house, and modify it.
4. Press F3 and note the two opposite corners of the house, including the roof and the basement if there is one.
5. Run `/theother export house_name x1 y1 z1 x2 y2 z2`.
6. Copy `.minecraft/theother-exports/house_name.nbt` and send it back.
7. The file is added to `src/main/resources/data/theother/structures/house_name.nbt`.
8. A later commit places it in the wheat world with `StructureTemplate` or a structure block.

### Export limits and caveats

- The command copies **air** inside the box. That is good for a house: the interior stays empty instead of being filled with terrain on placement.
- It copies **block entities** (chests, signs, furnaces) with their NBT.
- It does **not** copy entities (item frames, armor stands, villagers). Add those after placement if needed. This also prevents the player from being captured inside the selection.
- It does not preserve a structure block's metadata unless the block itself is inside the box. That is fine for a house.
- The selection must be loaded. The command loads chunks, but a very large box across many chunks can take a moment.
- The maximum is 128×128×128. A large mansion may need to be exported in pieces or trimmed to the house's footprint.
- The file is saved on the server's game directory. In singleplayer that is your `.minecraft`; on a dedicated server it is the server's working directory.

## Current limitations

- The wheat world is generated once per world. Changing the generator later does **not** regenerate an existing `TheOther` world. Delete `.minecraft/saves/TheOther` to regenerate it.
- The world seed is fixed, but the terrain function is coordinate-based, so the seed has no visible effect on the wheat.
- The Nether and End are normal dimensions and are reachable only by commands/portals. They are not part of the current slice.
- The house is not placed automatically. The export workflow is manual by design until the house is chosen.
- The one-life rule, the basement, the chest, and the forest are still design/docs only.

## Testing

```mcfunction
# After Play creates the world
/time set day
/gamemode survival
/tp @s 0 70 0

# Confirm the generator and biome
/execute in minecraft:overworld run locate biome theother:wheat_fields

# Export a 10x10x10 test cube
/theother export test_cube 0 64 0 10 74 10
```

Expected:

- The field is wheat in every direction, with gentle height changes.
- No trees, grass, or mobs.
- The spawn clearing is a small grass patch.
- `theother-exports/test_cube.nbt` exists and is non-empty.
- Placing the NBT with a structure block reproduces the cube.

## Build and CI

The project remains **CI-only**. Push to `main`, let the `Build` workflow compile, and download the `theother-jar` artifact. Do not run a local Gradle build.

## Sources

- `WorldOpenFlows` 1.20.1: <https://mappings.dev/1.20.1/net/minecraft/client/gui/screens/worldselection/WorldOpenFlows.html>
- `ChunkGenerator` 1.20.1: <https://mappings.dev/1.20.1/net/minecraft/world/level/chunk/ChunkGenerator.html>
- `StructureTemplate` 1.20.1: <https://mappings.dev/1.20.1/net/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate.html>
- Forge 1.20.1 documentation: <https://docs.minecraftforge.net/en/1.20.1/>
