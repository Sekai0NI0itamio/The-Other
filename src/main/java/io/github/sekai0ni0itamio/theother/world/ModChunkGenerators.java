package io.github.sekai0ni0itamio.theother.world;

import com.mojang.serialization.Codec;
import io.github.sekai0ni0itamio.theother.TheOther;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModChunkGenerators {
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, TheOther.MODID);

    public static final RegistryObject<Codec<? extends ChunkGenerator>> WHEAT =
            CHUNK_GENERATORS.register("wheat", () -> WheatChunkGenerator.CODEC);

    private ModChunkGenerators() {
    }

    public static void register(IEventBus modBus) {
        CHUNK_GENERATORS.register(modBus);
    }
}
