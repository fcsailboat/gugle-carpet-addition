package dev.dubhe.gugle.carpet.commands;

import carpet.fakes.ServerPlayerInterface;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.dubhe.gugle.carpet.GcaExtension;
import dev.dubhe.gugle.carpet.GcaSetting;
import dev.dubhe.gugle.carpet.tools.FakePlayerSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BotCommand {
    private static MinecraftServer server = null;
    private static final Map<String, BotInfo> BOT_INFO_MAP = new HashMap<>();
    private static final String BOT_GCA_JSON = "bot.gca.json";
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeHierarchyAdapter(ResourceKey.class, new DimTypeSerializer())
        .create();

    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bot").requires(sourceStack -> GcaSetting.commandBot)
            .then(
                Commands.literal("list").executes(BotCommand::listBot)
                    .then(
                        Commands.argument("page", IntegerArgumentType.integer(1))
                            .executes(BotCommand::listBot)
                    )
            )
            .then(
                Commands.literal("add")
                    .then(
                        Commands.argument("player", EntityArgument.player())
                            .then(
                                Commands.argument("desc", StringArgumentType.string())
                                    .executes(BotCommand::addBot)
                            )
                    )
            )
            .then(
                Commands.literal("load")
                    .then(
                        Commands.argument("player", StringArgumentType.string())
                            .suggests(BotCommand::suggestPlayer)
                            .executes(BotCommand::loadBot)
                    )
            )
            .then(
                Commands.literal("remove")
                    .then(
                        Commands.argument("player", StringArgumentType.string())
                            .suggests(BotCommand::suggestPlayer)
                            .executes(BotCommand::removeBot)
                    )
            )
        );
    }

    private static int listBot(CommandContext<CommandSourceStack> context) {
        BotCommand.init(context);
        int page;
        try {
            page = IntegerArgumentType.getInteger(context, "page");
        } catch (IllegalArgumentException ignored) {
            page = 1;
        }
        final int pageSize = 8;
        int size = BOT_INFO_MAP.size();
        int maxPage = size / pageSize + 1;
        BotInfo[] botInfos = BOT_INFO_MAP.values().toArray(new BotInfo[0]);
        context.getSource().sendSystemMessage(
            Component.literal("===== Bot List (Page %s/%s) =====".formatted(page, maxPage))
                .withStyle(ChatFormatting.YELLOW)
        );
        for (int i = (page - 1) * pageSize; i < size && i < page * pageSize; i++) {
            context.getSource().sendSystemMessage(botToComponent(botInfos[i]));
        }
        Component prevPage = page <= 1 ?
            Component.literal("<<<").withStyle(ChatFormatting.GRAY) :
            Component.literal("<<<").withStyle(
                Style.EMPTY
                    .applyFormat(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bot list " + (page - 1)))
            );
        Component nextPage = page >= maxPage ?
            Component.literal(">>>").withStyle(ChatFormatting.GRAY) :
            Component.literal(">>>").withStyle(
                Style.EMPTY
                    .applyFormat(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bot list " + (page + 1)))
            );
        context.getSource().sendSystemMessage(
            Component.literal("=======")
                .withStyle(ChatFormatting.YELLOW)
                .append(" ")
                .append(prevPage)
                .append(" ")
                .append(Component.literal("(Page %s/%s)".formatted(page, maxPage)).withStyle(ChatFormatting.YELLOW))
                .append(" ")
                .append(nextPage)
                .append(" ")
                .append(Component.literal("=======").withStyle(ChatFormatting.YELLOW))
        );
        return 1;
    }

    private static MutableComponent botToComponent(BotInfo botInfo) {
        MutableComponent component = Component.literal(botInfo.desc).withStyle(
            Style.EMPTY
                .applyFormat(ChatFormatting.GRAY)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(botInfo.name)))
        );
        MutableComponent load = Component.literal("[↑]").withStyle(
            Style.EMPTY
                .applyFormat(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bot load %s".formatted(botInfo.name)))
        );
        MutableComponent remove = Component.literal("[↓]").withStyle(
            Style.EMPTY
                .applyFormat(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/player %s kill".formatted(botInfo.name)))
        );
        return Component.literal("▶ ").append(component).append(" ").append(load).append(" ").append(remove);
    }

    private static int loadBot(CommandContext<CommandSourceStack> context) {
        BotCommand.init(context);
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "player");
        if (server.getPlayerList().getPlayerByName(name) != null) {
            source.sendFailure(Component.literal("player %s is already exist.".formatted(name)));
            return 0;
        }
        BotInfo botInfo = BOT_INFO_MAP.getOrDefault(name, null);
        if (botInfo == null) {
            source.sendFailure(Component.literal("%s is not exist."));
            return 0;
        }
        boolean success = EntityPlayerMPFake.createFake(
            name,
            server,
            botInfo.pos,
            botInfo.facing.y,
            botInfo.facing.x,
            botInfo.dimType,
            botInfo.mode,
            botInfo.flying
        );
        if (success) {
            if (botInfo.actions != null) {
                GcaExtension.ON_PLAYER_LOGGED_IN.put(
                    name,
                    (player) -> FakePlayerSerializer.applyActionPackFromJson(botInfo.actions, player)
                );
            }
            source.sendSuccess(() -> Component.literal("%s is loaded.".formatted(name)), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("%s is not loaded.".formatted(name)));
            return 0;
        }
    }

    private static int addBot(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BotCommand.init(context);
        CommandSourceStack source = context.getSource();
        if (!(EntityArgument.getEntity(context, "player") instanceof EntityPlayerMPFake player)) {
            source.sendFailure(Component.literal("%s is not a fake player."));
            return 0;
        }
        String name = player.getGameProfile().getName();
        if (BOT_INFO_MAP.containsKey(name)) {
            source.sendFailure(Component.literal("%s is already save."));
            return 0;
        }
        BotCommand.BOT_INFO_MAP.put(
            name,
            new BotInfo(
                name,
                StringArgumentType.getString(context, "desc"),
                player.position(),
                player.getRotationVector(),
                player.level().dimension(),
                player.gameMode.getGameModeForPlayer(),
                player.getAbilities().flying,
                FakePlayerSerializer.actionPackToJson(((ServerPlayerInterface) player).getActionPack())
            )
        );
        BotCommand.save();
        return 1;
    }

    private static int removeBot(CommandContext<CommandSourceStack> context) {
        BotCommand.init(context);
        String name = StringArgumentType.getString(context, "player");
        BotCommand.BOT_INFO_MAP.remove(name);
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("%s is removed."), false);
        BotCommand.save();
        return 1;
    }

    private static void init(@NotNull CommandContext<CommandSourceStack> context) {
        MinecraftServer server1 = context.getSource().getServer();
        BotCommand.init(server1);
    }

    public static void init(MinecraftServer server1) {
        if (server1 == server) return;
        BotCommand.server = server1;
        BotCommand.BOT_INFO_MAP.clear();
        File file = BotCommand.server.getWorldPath(LevelResource.ROOT).resolve(BotCommand.BOT_GCA_JSON).toFile();
        if (!file.isFile()) return;
        try (BufferedReader bfr = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            for (Map.Entry<String, JsonElement> entry : BotCommand.GSON.fromJson(bfr, JsonObject.class).entrySet()) {
                BotCommand.BOT_INFO_MAP.put(entry.getKey(), BotCommand.GSON.fromJson(entry.getValue(), BotInfo.class));
            }
        } catch (IOException e) {
            GcaExtension.LOGGER.error(e.getMessage(), e);
        }
    }

    private static @NotNull CompletableFuture<Suggestions> suggestPlayer(final CommandContext<CommandSourceStack> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(BOT_INFO_MAP.keySet(), builder);
    }

    private static void save() {
        if (BotCommand.server == null) return;
        File file = BotCommand.server.getWorldPath(LevelResource.ROOT).resolve(BotCommand.BOT_GCA_JSON).toFile();
        try (BufferedWriter bw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            BotCommand.GSON.toJson(BotCommand.BOT_INFO_MAP, bw);
        } catch (IOException e) {
            GcaExtension.LOGGER.error(e.getMessage(), e);
        }
    }

    public record BotInfo(
        String name,
        String desc,
        Vec3 pos,
        Vec2 facing,
        ResourceKey<Level> dimType,
        GameType mode,
        boolean flying,
        JsonObject actions
    ) {
    }

    public static class DimTypeSerializer implements JsonSerializer<ResourceKey<Level>>, JsonDeserializer<ResourceKey<Level>> {
        @Override
        public ResourceKey<Level> deserialize(@NotNull JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(json.getAsString()));
        }

        @Override
        public JsonElement serialize(@NotNull ResourceKey<Level> src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.location().toString());
        }
    }
}
