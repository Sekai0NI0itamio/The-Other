package io.github.sekai0ni0itamio.theother.client;

import io.github.sekai0ni0itamio.theother.TheOther;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheOther.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TheOtherClient {
    private TheOtherClient() {
    }

    @SubscribeEvent
    public static void replaceTitleScreen(ScreenEvent.Opening event) {
        if (event.getNewScreen() != null && event.getNewScreen().getClass() == TitleScreen.class) {
            event.setNewScreen(new TheOtherTitleScreen());
        }
    }
}
