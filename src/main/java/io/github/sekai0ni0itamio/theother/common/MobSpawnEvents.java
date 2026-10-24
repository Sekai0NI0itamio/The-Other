package io.github.sekai0ni0itamio.theother.common;

import io.github.sekai0ni0itamio.theother.TheOther;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheOther.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MobSpawnEvents {
    private MobSpawnEvents() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        MobSpawnType type = event.getSpawnType();
        if (type == MobSpawnType.COMMAND
                || type == MobSpawnType.SPAWN_EGG
                || type == MobSpawnType.BREEDING
                || type == MobSpawnType.MOB_SUMMONED) {
            return;
        }
        event.setSpawnCancelled(true);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        }
    }
}
