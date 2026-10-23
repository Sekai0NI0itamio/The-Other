# Assets and data: models, textures, sounds, loot, tags, and datagen

A Forge mod jar has two resource roots:

```text
src/main/resources/
├── assets/theother/     client resources
└── data/theother/       server data
```

If a file is in the wrong root, the game will not see it. This is the most common cause of "missing texture" and "unknown loot table" reports.

## Pack metadata

`src/main/templates/pack.mcmeta` is expanded by `generateModMetadata`:

```json
{
  "pack": {
    "description": {
      "text": "${mod_name} resources"
    },
    "pack_format": 15
  }
}
```

`pack_format: 15` is correct for 1.20.1. A mismatch produces a warning and may disable the pack. The file is in `templates/` because it contains `${mod_name}`; the build replaces it into the jar.

## Blocks: blockstate, model, texture

For a simple cube block (the stone rose statue):

`assets/theother/blockstates/rose_statue.json`:

```json
{
  "variants": {
    "": {
      "model": "theother:block/rose_statue"
    }
  }
}
```

`assets/theother/models/block/rose_statue.json`:

```json
{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "theother:block/rose_statue"
  }
}
```

`assets/theother/textures/block/rose_statue.png`:

- 16×16 for a vanilla-style block.
- Transparent pixels are allowed only if the model/render type supports them.
- Filenames and folders are lowercase; `ResourceLocation` is case-sensitive.

For a pot with a `filled` boolean property:

```json
{
  "variants": {
    "filled=false": { "model": "theother:block/rose_pot" },
    "filled=true": { "model": "theother:block/rose_pot_filled" }
  }
}
```

Register the blockstate property in code:

```java
public static final BooleanProperty FILLED = BooleanProperty.create("filled");

public final BlockState defaultBlockState() {
    return this.defaultBlockState().setValue(FILLED, false);
}
```

The exact registration depends on your block class; a `Block` subclass with `createBlockStateDefinition` is the standard route.

## Items: item model and texture

For a flat item (the enchanted rose):

`assets/theother/models/item/enchanted_rose.json`:

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "theother:item/enchanted_rose"
  }
}
```

`assets/theother/textures/item/enchanted_rose.png`:

- 16×16 vanilla-style item texture.
- The model parent `item/generated` expects `layer0`.
- A missing texture shows the purple/black checkerboard; a missing model shows a missing-model cube.

For a block item, use the block's model as the inventory model:

```json
{
  "parent": "theother:block/rose_statue"
}
```

## Language

`assets/theother/lang/en_us.json`:

```json
{
  "block.theother.rose_statue": "Rose Statue",
  "block.theother.rose_pot": "Rose Pot",
  "item.theother.enchanted_rose": "Enchanted Rose",
  "screen.theother.title": "The Other",
  "screen.theother.subtitle": "Do not open, or else",
  "theother.disconnect.life_spent": "The Other has taken its life. This world is closed.",
  "subtitles.theother.ambient.forest_whisper": "Something whispers"
}
```

- Block keys are `block.<namespace>.<path>`.
- Item keys are `item.<namespace>.<path>`.
- Screen keys are free-form; use `screen.<namespace>.<name>`.
- Subtitle keys are referenced from `sounds.json` via `"subtitle": "subtitles.theother..."`.
- Use `Component.translatable(...)` in code, not `Component.literal(...)`, for anything the player sees.

## Sounds

`sounds.json` maps a sound event id to one or more OGG files and a category:

```json
{
  "ambient.forest_whisper": {
    "category": "ambient",
    "subtitle": "subtitles.theother.ambient.forest_whisper",
    "sounds": [
      "theother:ambient/forest_whisper_1",
      "theother:ambient/forest_whisper_2"
    ]
  }
}
```

The files:

```text
assets/theother/sounds/ambient/forest_whisper_1.ogg
assets/theother/sounds/ambient/forest_whisper_2.ogg
```

- The path in `sounds.json` is relative to `assets/theother/sounds/`, without the `.ogg` extension.
- OGG Vorbis is the supported format.
- `category` should reflect the mixer channel (`ambient`, `music`, `master`, `player`, `block`, `hostile`, `neutral`, `weather`, `ui`).
- The subtitle key must exist in `lang/en_us.json`; otherwise the subtitle shows the raw key.
- A malformed `sounds.json` logs a parse error and can disable all sounds in the file. Validate the JSON before shipping.

## Loot tables

`data/theother/loot_tables/chests/rose_chest.json`:

```json
{
  "type": "minecraft:chest",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "theother:enchanted_rose"
        }
      ]
    }
  ]
}
```

- The path after `loot_tables/` is the id: `theother:chests/rose_chest`.
- A chest NBT uses `LootTable: "theother:chests/rose_chest"`.
- `rolls: 1` guarantees the pool rolls once. With one entry, the chest contains exactly one rose.
- For a guaranteed single item, `rolls` is enough. Do not add a second entry unless you want randomness.
- Loot tables support functions (`set_count`, `set_nbt`, `enchant_with_levels`). The enchanted rose can be a custom item rather than a vanilla item with NBT; a custom item is easier to detect in code.

## Tags

Tags are lists of registry ids, used by structures, recipes, and blocks.

`data/theother/tags/worldgen/biome/has_structure/rose_podium.json`:

```json
{
  "values": [
    "theother:ashen_forest"
  ]
}
```

`data/theother/tags/blocks/rose_soil.json`:

```json
{
  "values": [
    "minecraft:grass_block",
    "minecraft:dirt",
    "minecraft:mud"
  ]
}
```

- Tag folder names are plural: `tags/blocks`, `tags/items`, `tags/worldgen/biome`.
- A tag can use `"required": false` on an entry if it is optional. For a mod's own content, keep entries required so a typo fails loudly.
- A structure JSON's `biomes` field points at the biome tag id, not the biome id.

## Recipes

`data/theother/recipes/rose_pot.json`:

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    "S S",
    " S "
  ],
  "key": {
    "S": { "item": "minecraft:stone" }
  },
  "result": {
    "item": "theother:rose_pot"
  }
}
```

- 1.20.1 uses `"item"` in ingredients; 1.21 changed some formats. Do not copy newer recipes blindly.
- A recipe id is the file path: `theother:rose_pot`.
- If the result is a block, use the block item id.

## Advancements

`data/theother/advancements/story/open_the_chest.json`:

```json
{
  "display": {
    "icon": { "item": "theother:enchanted_rose" },
    "title": { "translate": "advancements.theother.open_the_chest.title" },
    "description": { "translate": "advancements.theother.open_the_chest.description" },
    "frame": "task",
    "show_toast": true,
    "announce_to_chat": false,
    "hidden": true
  },
  "criteria": {
    "opened": {
      "trigger": "minecraft:player_generates_container_loot",
      "conditions": {
        "loot_table": "theother:chests/rose_chest"
      }
    }
  }
}
```

Advancements are optional for the core loop; use them for subtle guidance, not for loud quest popups. `hidden: true` keeps the tone.

## Data generation

The build already defines a `data` run:

```groovy
data {
    data()
    programArguments.addAll '--mod', mod_id, '--all',
            '--output', file('src/generated/resources/').getAbsolutePath(),
            '--existing', file('src/main/resources/').getAbsolutePath()
}
```

The `data` run executes the `GatherDataEvent` providers and writes to `src/generated/resources`, which is added to the main resource set. In this project, treat data generation like a build: run it in CI (a `workflow_dispatch` job) rather than on the workstation.

A minimal provider registration:

```java
@Mod.EventBusSubscriber(modid = TheOther.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModData {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(),
                new ModItemModelProvider(output, helper));
        generator.addProvider(event.includeClient(),
                new ModBlockStateProvider(output, helper));
        generator.addProvider(event.includeClient(),
                new ModLanguageProvider(output));

        generator.addProvider(event.includeServer(),
                new ModRecipeProvider(output));
        generator.addProvider(event.includeServer(),
                new ModLootTableProvider(output));
    }
}
```

Provider skeletons:

```java
public final class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, TheOther.MODID, helper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.ENCHANTED_ROSE.get());
    }
}
```

```java
public final class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, TheOther.MODID, helper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlock(ModBlocks.ROSE_STATUE.get());
    }
}
```

```java
public final class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, TheOther.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        addBlock(ModBlocks.ROSE_STATUE, "Rose Statue");
        addItem(ModItems.ENCHANTED_ROSE, "Enchanted Rose");
        add("screen.theother.title", "The Other");
    }
}
```

Notes:

- `PackOutput` is `net.minecraft.data.PackOutput` in 1.20.1.
- `ExistingFileHelper` is `net.minecraftforge.common.data.ExistingFileHelper`.
- `ItemModelProvider#basicItem(Item)` and `BlockStateProvider#simpleBlock(Block)` are the shortcuts.
- `LanguageProvider#addBlock`/`addItem` produce the correct keys.
- `GatherDataEvent` is a mod-bus event. Register the class with `@Mod.EventBusSubscriber(bus = Bus.MOD)`.
- Datagen is optional. Hand-written JSON is fine for a small mod and easier to review.
- Generated resources are source files. Commit them if the project ships them; ignore the `.cache` folder.

## Reload behavior

| Content | `/reload` | Restart world |
| --- | --- | --- |
| Recipes, loot tables, tags, advancements | Yes | Yes |
| Biome/structure JSON | Usually | Safest |
| Dimensions and dimension types | No | Required |
| Models and textures | Yes (resource reload) | Yes |
| Sounds | Yes (resource reload) | Yes |
| Chunk generators / codecs | No | Required |

If a data change does not appear, restart before debugging the JSON. A stale dimension is the usual culprit.

## Debugging missing assets

| Symptom | Check |
| --- | --- |
| Purple/black checkerboard item | `models/item/<id>.json`, `textures/item/<id>.png`, `layer0` path |
| Purple/black block | Blockstate path, model path, texture path, `parent` |
| "Missing model" in log | The model id in the blockstate does not resolve |
| "Unable to load model" | JSON syntax or a bad `parent`/`textures` key |
| Missing texture but model loads | The texture path in `textures` is wrong or the file is not a PNG |
| Sound does not play | `sounds.json` syntax, OGG path, registry name, subtitle key |
| Loot table missing | `data/theother/loot_tables/...`, wrong `LootTable` id |
| Lang shows a key | Key mismatch or the lang file is not in `assets/theother/lang/en_us.json` |
| Pack format warning | `pack_format` is not 15 for 1.20.1 |

Resource locations are lowercase and use `/`, never `\`. A single uppercase letter in a path creates a separate id that will never resolve on a case-sensitive filesystem (including most servers and CI).

## Asset licensing

- Do not copy vanilla textures, sounds, or models into the mod. They are Mojang's assets and are not redistributed with mods.
- Do not copy another mod's assets without permission, even if the repository is public.
- Use original art, commissioned art, or assets with a compatible licence. Record the licence and attribution in `CREDITS.md`.
- The template files are MIT; the mod's code and assets are All Rights Reserved until you choose otherwise.

## Verification

- [ ] Every registered item has a model and texture.
- [ ] Every registered block has a blockstate, model, and texture.
- [ ] Every sound has an OGG, a `sounds.json` entry, a `SoundEvent`, and a subtitle key.
- [ ] Every loot table path matches the NBT `LootTable` tag.
- [ ] Every structure's `biomes` tag exists and contains the intended biome.
- [ ] `runData` (or the CI artifact) produces the expected files.
- [ ] A resource reload does not log missing-model or missing-sound warnings.
- [ ] The GitHub Actions build is green.

## Sources

- Minecraft Wiki, resource pack: <https://minecraft.wiki/w/Resource_pack>
- Minecraft Wiki, model: <https://minecraft.wiki/w/Model>
- Minecraft Wiki, sounds.json: <https://minecraft.wiki/w/Sounds.json>
- Minecraft Wiki, loot table: <https://minecraft.wiki/w/Loot_table>
- Forge 1.20.1 datagen: <https://docs.minecraftforge.net/en/1.20.1/datagen/>
- Forge 1.20.1 `GatherDataEvent`: <https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/data/event/GatherDataEvent.java>
