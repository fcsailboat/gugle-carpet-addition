package dev.dubhe.gugle.carpet.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.dubhe.gugle.carpet.GcaSetting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BotCommand {
    private static final Map<String, BotInfo> BOT_INFO_MAP = new HashMap<>();

    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bot").requires(sourceStack -> GcaSetting.commandBot)
            .then(
                Commands.literal("list")
            )
            .then(
                Commands.literal("add")
                    .then(
                        Commands.argument("player", EntityArgument.player())
                            .then(
                                Commands.argument("desc", StringArgumentType.string())
                            )
                    )
            )
            .then(
                Commands.literal("remove")
                    .then(
                        Commands.argument("player", StringArgumentType.string()).suggests(BotCommand::suggestPlayer)
                    )
            )
        );
    }

    public static @NotNull CompletableFuture<Suggestions> suggestPlayer(final CommandContext<CommandSourceStack> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(BOT_INFO_MAP.keySet(), builder);
    }

    public static class BotInfo {
        String name;
        String desc;
        Vec3 pos;
    }
}
