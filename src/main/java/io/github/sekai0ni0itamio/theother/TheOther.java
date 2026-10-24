package io.github.sekai0ni0itamio.theother;

import com.mojang.logging.LogUtils;
import io.github.sekai0ni0itamio.theother.world.ModChunkGenerators;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(TheOther.MODID)
public final class TheOther {
    public static final String MODID = "theother";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TheOther() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModChunkGenerators.register(modBus);
        LOGGER.info("The Other is loading");
    }
}
