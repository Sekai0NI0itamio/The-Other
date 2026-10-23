# References

Primary documentation and data sources used by this project. Prefer these over random tutorials; 1.20.1 APIs differ from 1.21 and from NeoForge.

## Forge 1.20.1

- Forge documentation: <https://docs.minecraftforge.net/en/1.20.1/>
- Forge 1.20.1 source: <https://github.com/MinecraftForge/MinecraftForge/tree/1.20.1>
- Forge 1.20.1 MDK: <https://github.com/MinecraftForge/MinecraftForge/tree/1.20.1/mdk>
- Forge downloads: <https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html>
- Forge Maven metadata: <https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml>
- Forge events: <https://docs.minecraftforge.net/en/1.20.1/events/>
- Forge sides: <https://docs.minecraftforge.net/en/1.20.1/concepts/sides/>
- Forge registries: <https://docs.minecraftforge.net/en/1.20.1/concepts/registries/>
- Forge datagen: <https://docs.minecraftforge.net/en/1.20.1/datagen/>
- Forge access transformers: <https://docs.minecraftforge.net/en/1.20.1/advanced/accesstransformers/>
- Forge networking: <https://docs.minecraftforge.net/en/1.20.1/networking/>

## Build system

- ModDevGradle Legacy: <https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md>
- ModDevGradle: <https://github.com/neoforged/ModDevGradle>
- NeoForged 1.20.1 Forge template: <https://github.com/NeoForgeMDKs/MDK-Forge-1.20.1-ModDevGradle>
- Parchment: <https://parchmentmc.org/docs/getting-started>
- Gradle wrapper: <https://docs.gradle.org/current/userguide/gradle_wrapper.html>
- GitHub Actions: <https://docs.github.com/en/actions>

## Mappings and API lookup

- Mappings viewer: <https://mappings.dev/1.20.1/>
- Parchment downloads: <https://parchmentmc.org/docs/getting-started>
- Forge Javadoc index: <https://docs.minecraftforge.net/en/1.20.1/javadoc/>

## World generation data

- Vanilla 1.20.1 data pack: <https://github.com/misode/mcmeta/tree/1.20.1-data>
- Vanilla worldgen: <https://github.com/misode/mcmeta/tree/1.20.1-data/data/minecraft/worldgen>
- Vanilla dimensions: <https://github.com/misode/mcmeta/tree/1.20.1-data/data/minecraft/dimension>
- Vanilla dimension types: <https://github.com/misode/mcmeta/tree/1.20.1-data/data/minecraft/dimension_type>
- Misode's worldgen generators: <https://misode.github.io/worldgen/>
- Misode's loot table generator: <https://misode.github.io/loot-table/>
- Misode's recipe generator: <https://misode.github.io/recipe/>

## Minecraft Wiki

- Custom dimension: <https://minecraft.wiki/w/Custom_dimension>
- Dimension type: <https://minecraft.wiki/w/Dimension_type>
- World generation: <https://minecraft.wiki/w/World_generation>
- Structure: <https://minecraft.wiki/w/Structure>
- Biome: <https://minecraft.wiki/w/Biome>
- Resource pack: <https://minecraft.wiki/w/Resource_pack>
- Model: <https://minecraft.wiki/w/Model>
- Sounds.json: <https://minecraft.wiki/w/Sounds.json>
- Loot table: <https://minecraft.wiki/w/Loot_table>
- Title screen: <https://minecraft.wiki/w/Title_screen>
- Fog: <https://minecraft.wiki/w/Fog>
- Hardcore: <https://minecraft.wiki/w/Hardcore>

## Mods referenced by the plan

- The Boiled One: <https://www.curseforge.com/minecraft/search?search=The%20Boiled%20One>
- Twixxel's Stalker: <https://www.curseforge.com/minecraft/search?search=Twixxel%27s%20Stalker>

## Performance and pack tooling

- Embeddium: <https://www.curseforge.com/minecraft/mc-mods/embeddium>
- Oculus: <https://www.curseforge.com/minecraft/mc-mods/oculus>
- FerriteCore: <https://www.curseforge.com/minecraft/mc-mods/ferritecore>
- ModernFix: <https://www.curseforge.com/minecraft/mc-mods/modernfix>
- Canary: <https://www.curseforge.com/minecraft/mc-mods/canary>
- Starlight: <https://www.curseforge.com/minecraft/mc-mods/starlight-forge>
- CurseForge manifest format: <https://support.curseforge.com/en/support/solutions/articles/9000197913-modpack-manifest>
- Modrinth pack format: <https://docs.modrinth.com/docs/modpacks/format/>

## Repository

- GitHub: <https://github.com/Sekai0NI0itamio/The-Other>

## How to use a reference

1. Check the version selector. A 1.21 page is not a 1.20.1 page.
2. Check the loader. NeoForge examples use `DeferredHolder`, `CustomPacketPayload`, and other APIs that do not exist in 1.20.1 Forge.
3. Check the mappings. A Yarn/Fabric name will not compile against Mojang mappings.
4. When a class moved, search the 1.20.1 package in `mappings.dev` rather than trusting a blog post.
5. If a source conflicts with the compiler, the compiler wins. Fix the doc after you find the real signature.
