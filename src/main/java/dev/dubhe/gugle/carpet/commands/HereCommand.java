package dev.dubhe.gugle.carpet.commands;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.dubhe.gugle.carpet.GcaSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class HereCommand {
    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("here")
                .requires(stack -> CommandHelper.canUseCommand(stack, GcaSetting.commandHere))
                .executes(HereCommand::execute)
        );
    }

    public static int execute(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) return 0;
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        server.getPlayerList().broadcastSystemMessage(HereCommand.playerPos(player), false);
        return 1;
    }

    public static MutableComponent playerPos(ServerPlayer player) {
        Vec3 position = player.position();
        ResourceKey<Level> dimension = player.level().dimension();
        String name = player.getGameProfile().getName();
        MutableComponent pos = Component.literal("[%.2f, %.2f, %.2f]".formatted(position.x, position.y, position.z)).withStyle(
            Style.EMPTY
                .applyFormat(
                    dimension == Level.OVERWORLD ?
                        ChatFormatting.GREEN :
                        dimension == Level.NETHER ?
                            ChatFormatting.RED :
                            dimension == Level.END ?
                                ChatFormatting.LIGHT_PURPLE :
                                ChatFormatting.AQUA
                )
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(dimension.location().toString())))
        );
        MutableComponent addMap = Component.literal("[+X]").withStyle(
            Style.EMPTY
                .applyFormat(ChatFormatting.GREEN)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Add to Xaero's minimap")))
                .withClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/xaero_waypoint_add:%s:%s:%s:%s:%s:0:false:0:Internal_%s_waypoints".formatted(
                        name,
                        name.substring(0, 1),
                        (int) position.x,
                        (int) position.y,
                        (int) position.z,
                        dimension.location().getPath()
                    )
                ))
        );
        return Component.literal("%s at".formatted(name))
            .append(" ").append(pos)
            .append(" ").append(addMap);
    }
}
