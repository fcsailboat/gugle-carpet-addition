package dev.dubhe.gugle.carpet.commands;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.dubhe.gugle.carpet.GcaSetting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class WhereisCommand {
    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("whereis")
                .requires(stack -> CommandHelper.canUseCommand(stack, GcaSetting.commandWhereis))
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .executes(WhereisCommand::execute)
                )
        );
    }

    public static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        context.getSource().sendSuccess(() -> HereCommand.playerPos(player), false);
        return 1;
    }
}
