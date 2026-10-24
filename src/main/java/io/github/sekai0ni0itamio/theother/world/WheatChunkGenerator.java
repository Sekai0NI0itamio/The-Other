package io.github.sekai0ni0itamio.theother.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class WheatChunkGenerator extends ChunkGenerator {
    public static final Codec<WheatChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, WheatChunkGenerator::new));

    private static final int BASE_HEIGHT = 64;
    private static final int MIN_Y = -64;
    private static final int HEIGHT = 384;
    private static final int SEA_LEVEL = 64;

    public WheatChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random,
                                                        StructureManager structures, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState farmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7);
        BlockState wheat = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);

        int minY = chunk.getMinBuildHeight();

        for (int localX = 0; localX < 16; localX++) {
            int worldX = chunkPos.getMinBlockX() + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = chunkPos.getMinBlockZ() + localZ;
                int surface = surfaceY(worldX, worldZ);

                for (int y = minY; y < surface; y++) {
                    BlockState state;
                    if (y == minY) {
                        state = bedrock;
                    } else if (y < surface - 3) {
                        state = stone;
                    } else {
                        state = dirt;
                    }
                    chunk.setBlockState(cursor.set(worldX, y, worldZ), state, false);
                }

                chunk.setBlockState(cursor.set(worldX, surface, worldZ), farmland, false);
                chunk.setBlockState(cursor.set(worldX, surface + 1, worldZ), wheat, false);
            }
        }

        Heightmap.primeHeightmaps(chunk, EnumSet.of(
                Heightmap.Types.WORLD_SURFACE_WG,
                Heightmap.Types.OCEAN_FLOOR_WG
        ));

        return CompletableFuture.completedFuture(chunk);
    }

    private static int surfaceY(int x, int z) {
        double wave = Math.sin(x * 0.025D) * 1.5D
                + Math.cos(z * 0.021D) * 1.5D
                + Math.sin((x + z) * 0.009D) * 1.0D;
        return BASE_HEIGHT + Mth.floor(wave);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomeManager,
                             StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return SEA_LEVEL;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor level) {
        return surfaceY(0, 0) + 2;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return surfaceY(x, z) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int minY = level.getMinBuildHeight();
        BlockState[] column = new BlockState[level.getHeight()];
        int surface = surfaceY(x, z);

        for (int index = 0; index < column.length; index++) {
            int y = minY + index;
            if (y < surface - 3) {
                column[index] = Blocks.STONE.defaultBlockState();
            } else if (y < surface) {
                column[index] = Blocks.DIRT.defaultBlockState();
            } else if (y == surface) {
                column[index] = Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7);
            } else {
                column[index] = Blocks.AIR.defaultBlockState();
            }
        }

        return new NoiseColumn(minY, column);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
    }
}
