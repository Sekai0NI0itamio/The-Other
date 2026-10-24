package io.github.sekai0ni0itamio.theother.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.sekai0ni0itamio.theother.TheOther;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = TheOther.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ExportCommand {
    private static final int MAX_SIZE = 128;

    private ExportCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("theother").then(exportCommand()));
        event.getDispatcher().register(Commands.literal("housecopy").then(exportCommand()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> exportCommand() {
        return Commands.literal("export")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("from", BlockPosArgument.blockPos())
                                .then(Commands.argument("to", BlockPosArgument.blockPos())
                                        .executes(ExportCommand::run))));
    }

    private static int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        BlockPos from = BlockPosArgument.getBlockPos(context, "from");
        BlockPos to = BlockPosArgument.getBlockPos(context, "to");

        int minX = Math.min(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxX = Math.max(from.getX(), to.getX());
        int maxY = Math.max(from.getY(), to.getY());
        int maxZ = Math.max(from.getZ(), to.getZ());

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        if (sizeX > MAX_SIZE || sizeY > MAX_SIZE || sizeZ > MAX_SIZE) {
            context.getSource().sendFailure(Component.translatable("theother.command.export.too_large", MAX_SIZE));
            return 0;
        }

        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }

        String name = StringArgumentType.getString(context, "name");
        StructureTemplate template = new StructureTemplate();
        template.setAuthor(context.getSource().getTextName());
        template.fillFromWorld(level, new BlockPos(minX, minY, minZ), new Vec3i(sizeX, sizeY, sizeZ), false, null);
        CompoundTag tag = template.save(new CompoundTag());

        Path directory = FMLPaths.GAMEDIR.get().resolve("theother-exports");
        try {
            Files.createDirectories(directory);
            NbtIo.writeCompressed(tag, directory.resolve(name + ".nbt").toFile());
            context.getSource().sendSuccess(
                    () -> Component.translatable("theother.command.export.success", name, sizeX, sizeY, sizeZ, name),
                    false
            );
            return 1;
        } catch (IOException exception) {
            TheOther.LOGGER.error("Failed to export structure {}", name, exception);
            context.getSource().sendFailure(Component.translatable("theother.command.export.failed", exception.getMessage()));
            return 0;
        }
    }
}
