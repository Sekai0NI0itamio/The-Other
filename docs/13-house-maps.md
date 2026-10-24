# House maps: five free downloads for level 1

Five free maps were downloaded for the peaceful wheat level. The goal is a detailed wooden house that can be opened in 1.20.1, edited in-game, and exported with `/theother export`.

The archives are stored locally in `maps/` and are **not committed** to the repository (see `.gitignore`). They are source material, not redistributable pack assets. Download them again from the links below if the local folder is missing.

## The five maps

| # | Map | Style | Archive | Size | World name | Data version | Spawn | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | **Small Starter House** | Cozy taiga wooden cabin; screenshots show a wheat farm and cow pen | `small-starter-house.zip` | 31 MB | `Small Starter House 1.0.1` | 3465 (1.20.1) | `-309, 64, 19` | <https://www.9minecraft.net/small-starter-house-map> |
| 2 | **Ultimate Survival House** | Large, detailed wooden survival house | `ultimate-survival-house.zip` | 17 MB | `Huge Survival House` | 3465 (1.20.1) | `0, 69, 32` | <https://www.planetminecraft.com/project/ultimate-survival-house-download/> |
| 3 | **Survival Big House** | Big wooden survival house for 2–3 players | `survival-big-house.zip` | 8.6 MB | `CASE` | 2578 (older; upgrades) | `128, 70, 144` | <https://www.9minecraft.net/survival-big-house-map> |
| 4 | **Minecraft House** | Two-story American wooden house with a yard; classic bones | `minecraft-house.zip` | 5.0 MB | `Home` | 2730 (older; upgrades) | `-16, 4, 128` | <https://www.9minecraft.net/minecraft-house-map> |
| 5 | **Cozy Cake House** | Small whimsical wooden starter home | `cozy-cake-house.zip` | 51 MB | `Cozy Cake House 1.0.5` | 3465 (1.20.1) | `-220, 64, -41` | <https://www.9minecraft.net/cozy-cake-house-map/> |

Data versions were read from each `level.dat`:

- `3465` is Minecraft 1.20.1.
- `2730` and `2578` are older worlds. 1.20.1 opens and upgrades them on first load; test the house before exporting. A backup of the archive is kept, so a failed upgrade cannot destroy the only copy.

## Which one fits level 1

**Best fit: Small Starter House.** It is already a small wooden cabin in a field, and its screenshots include a wheat farm and animal pen. The scale matches "one house in the distance" in a wheat world.

**Most detailed: Ultimate Survival House.** The best choice if the house should feel authored and slightly too large for the player. It is also natively 1.20.1.

**Biggest: Survival Big House.** Useful if the house is meant to hold a basement, a chest room, and a story route. It may be too large for a lone wheat field.

**Classic: Minecraft House.** A two-story wooden house with a yard and garage. It reads as a normal home, which makes the secret basement more effective.

**Wildcard: Cozy Cake House.** The least horror-appropriate, but its "too perfect" look can work in the peaceful world before the reveal.

## Install the maps

Unzip each archive into the instance's `saves/` folder. Each archive already contains a top-level world folder.

```bash
for archive in maps/*.zip; do
  unzip -q "$archive" -d "/path/to/.minecraft/saves"
done
```

On macOS with the default launcher, the path is usually:

```text
~/Library/Application Support/minecraft/saves
```

On Windows:

```text
%APPDATA%\.minecraft\saves
```

Open the world in the 1.20.1 Forge instance with The Other installed. Older worlds will show a version warning; let Minecraft upgrade them. If the upgrade looks wrong, delete the upgraded world and unzip the archive again.

## Workflow: from map to structure

1. Choose one house.
2. Open the world in 1.20.1 Forge with The Other installed.
3. Modify the house in-game: walls, roof, basement entrance, carpets, chest, sign, and the trapdoor.
4. Press F3 and note two opposite corners. Include the full roof and the basement if it is part of the house.
5. Run:

```mcfunction
/theother export house_name x1 y1 z1 x2 y2 z2
```

6. The command writes `.minecraft/theother-exports/house_name.nbt`.
7. Send that file back.
8. It is added to `src/main/resources/data/theother/structures/house_name.nbt` and placed in the wheat world in a later commit.

## Notes on fit and scale

- The export command is capped at 128 blocks per axis. Check the house size before selecting corners.
- A house with a deep basement may need two exports: one for the house, one for the basement room.
- The selection includes air, block entities, and entities. Chests and signs keep their NBT; item frames and armor stands are included if they are inside the box.
- Do not include a large slice of surrounding terrain. The wheat world already provides the ground; the house should sit on it.
- If the house has a custom resource pack, note it. The structure itself only stores block states; a missing mod block would become air or a missing block on placement.

## Licensing and redistribution

- The maps are free to download for personal use, but that does **not** mean their assets can be redistributed in a public modpack.
- Do not commit the map archives, extracted worlds, or copied builds to the public repository.
- A house built by a map author is their creative work. If the final modpack ships the house, ask the author for permission and credit them, or use a house built specifically for the project.
- The structure NBT exported from your own edited copy is still derived from the original build. Treat it as an attribution-required asset until permission is clear.

## Sources

- Small Starter House: <https://www.9minecraft.net/small-starter-house-map>
- Ultimate Survival House: <https://www.planetminecraft.com/project/ultimate-survival-house-download/>
- Survival Big House: <https://www.9minecraft.net/survival-big-house-map>
- Minecraft House: <https://www.9minecraft.net/minecraft-house-map>
- Cozy Cake House: <https://www.9minecraft.net/cozy-cake-house-map/>
- Planet Minecraft install guide: <https://www.planetminecraft.com/blog/how-to-download-and-install-minecraft-maps/>
- Minecraft Wiki, structure block: <https://minecraft.wiki/w/Structure_Block>
