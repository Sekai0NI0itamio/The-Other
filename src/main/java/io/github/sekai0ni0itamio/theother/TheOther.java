package io.github.sekai0ni0itamio.theother;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TheOther.MODID)
public final class TheOther {
    public static final String MODID = "theother";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TheOther() {
        LOGGER.info("The Other is loading");
    }
}
