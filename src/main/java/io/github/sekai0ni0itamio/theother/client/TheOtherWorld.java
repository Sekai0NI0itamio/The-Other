package io.github.sekai0ni0itamio.theother.client;

import io.github.sekai0ni0itamio.theother.TheOther;
import io.github.sekai0ni0itamio.theother.world.WheatChunkGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

public final class TheOtherWorld {
    public static final String FOLDER = "TheOther";
    public static final String DISPLAY_NAME = "The Other";
    private static final long SEED = 0L;
    private static final ResourceKey<Biome> WHEAT_FIELDS = ResourceKey.create(
            Registries.BIOME,
            new ResourceLocation(TheOther.MODID, "wheat_fields")
    );

    private TheOtherWorld() {
    }

    public static void play(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getLevelSource().levelExists(FOLDER)) {
            minecraft.createWorldOpenFlows().loadLevel(parent, FOLDER);
            return;
        }

        LevelSettings settings = new LevelSettings(
                DISPLAY_NAME,
                GameType.SURVIVAL,
                false,
                Difficulty.PEACEFUL,
                true,
                new GameRules(),
                WorldDataConfiguration.DEFAULT
        );
        WorldOptions options = new WorldOptions(SEED, false, false);
        minecraft.createWorldOpenFlows().createFreshLevel(FOLDER, settings, options, TheOtherWorld::dimensions);
    }

    private static WorldDimensions dimensions(RegistryAccess access) {
        Holder<Biome> biome = access.registryOrThrow(Registries.BIOME).getHolderOrThrow(WHEAT_FIELDS);
        ChunkGenerator generator = new WheatChunkGenerator(new FixedBiomeSource(biome));
        return WorldPresets.createNormalWorldDimensions(access).replaceOverworldGenerator(access, generator);
    }
}
