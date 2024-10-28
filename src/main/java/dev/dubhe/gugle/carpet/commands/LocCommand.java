package dev.dubhe.gugle.carpet.commands;

import carpet.utils.CommandHelper;
import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.dubhe.gugle.carpet.GcaExtension;
import dev.dubhe.gugle.carpet.GcaSetting;
import dev.dubhe.gugle.carpet.tools.FilesUtil;
import dev.dubhe.gugle.carpet.tools.IdGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class LocCommand {
    public static final FilesUtil<Long, LocPoint> LOC_POINT = new FilesUtil<>("loc", Long::decode, LocPoint.class);

    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("loc")
                .requires(stack -> CommandHelper.canUseCommand(stack, GcaSetting.commandLoc))
                .executes(LocCommand::list)
                .then(
                    Commands.literal("add")
                        .then(
                            Commands.argument("desc", StringArgumentType.greedyString())
                                .executes(LocCommand::add)
                                .then(
                                    Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(LocCommand::add)
                                        .then(
                                            Commands.argument("dim", DimensionArgument.dimension())
                                                .executes(LocCommand::add)
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("remove")
                        .then(
                            Commands.argument("id", LongArgumentType.longArg())
                                .suggests(LocCommand::suggestId)
                                .executes(LocCommand::remove)
                        )
                )
                .then(
                    Commands.literal("list")
                        .executes(LocCommand::list)
                        .then(
                            Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(LocCommand::list)
                        )
                )
        );
    }

    private static @NotNull CompletableFuture<Suggestions> suggestId(
        final CommandContext<CommandSourceStack> context,
        final SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(LOC_POINT.map.keySet().stream().map(Object::toString), builder);
    }

    public static int add(CommandContext<CommandSourceStack> context) {
        LOC_POINT.init(context);
        CommandSourceStack source = context.getSource();
        long id = IdGenerator.nextId();
        String desc = StringArgumentType.getString(context, "desc");
        Vec3 pos;
        try {
            pos = Vec3Argument.getVec3(context, "pos");
        } catch (IllegalArgumentException ignored) {
            pos = source.getPosition();
        }
        ResourceKey<Level> dim;
        try {
            dim = DimensionArgument.getDimension(context, "dim").dimension();
        } catch (IllegalArgumentException ignored) {
            dim = source.getLevel().dimension();
        } catch (CommandSyntaxException e) {
            GcaExtension.LOGGER.error(e.getMessage(), e);
            dim = source.getLevel().dimension();
        }
        LOC_POINT.map.put(id, new LocPoint(id, desc, pos.x, pos.y, pos.z, dim));
        LOC_POINT.save();
        source.sendSuccess(() -> Component.literal("Loc %s is added.".formatted(desc)), false);
        return 1;
    }

    public static int remove(CommandContext<CommandSourceStack> context) {
        LOC_POINT.init(context);
        Long id = LongArgumentType.getLong(context, "id");
        LocPoint remove = LOC_POINT.map.remove(id);
        if (remove == null) {
            context.getSource().sendFailure(Component.literal("No such loc id %s".formatted(id)));
            return 0;
        }
        LOC_POINT.save();
        context.getSource().sendSuccess(() -> Component.literal("Loc %s is removed.".formatted(remove.desc)), false);
        return 1;
    }

    public static int list(CommandContext<CommandSourceStack> context) {
        LOC_POINT.init(context);
        int page;
        try {
            page = IntegerArgumentType.getInteger(context, "page");
        } catch (IllegalArgumentException ignored) {
            page = 1;
        }
        final int pageSize = 8;
        int size = LOC_POINT.map.size();
        int maxPage = size / pageSize + 1;
        LocPoint[] locPoints = LOC_POINT.map.values().toArray(new LocPoint[0]);
        context.getSource().sendSystemMessage(
            Component.literal("======= Loc List (Page %s/%s) =======".formatted(page, maxPage))
                .withStyle(ChatFormatting.YELLOW)
        );
        for (int i = (page - 1) * pageSize; i < size && i < page * pageSize; i++) {
            context.getSource().sendSystemMessage(locToComponent(locPoints[i]));
        }
        Component prevPage = page <= 1 ?
            Component.literal("<<<").withStyle(ChatFormatting.GRAY) :
            Component.literal("<<<").withStyle(
                Style.EMPTY
                    .applyFormat(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/loc list " + (page - 1)))
            );
        Component nextPage = page >= maxPage ?
            Component.literal(">>>").withStyle(ChatFormatting.GRAY) :
            Component.literal(">>>").withStyle(
                Style.EMPTY
                    .applyFormat(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/loc list " + (page + 1)))
            );
        context.getSource().sendSystemMessage(
            Component.literal("=======")
                .withStyle(ChatFormatting.YELLOW)
                .append(" ")
                .append(prevPage)
                .append(" ")
                .append(Component.literal("(Loc %s/%s)".formatted(page, maxPage)).withStyle(ChatFormatting.YELLOW))
                .append(" ")
                .append(nextPage)
                .append(" ")
                .append(Component.literal("=======").withStyle(ChatFormatting.YELLOW))
        );
        return 1;
    }

    private static @NotNull MutableComponent locToComponent(LocPoint locPoint) {
        MutableComponent component = Component.literal(locPoint.desc).withStyle(
            Style.EMPTY
                .applyFormat(ChatFormatting.GRAY)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(Long.toString(locPoint.id))))
        );
        MutableComponent pos = Component.literal("[%.2f, %.2f, %.2f]".formatted(locPoint.x, locPoint.y, locPoint.z)).withStyle(
            Style.EMPTY
                .applyFormat(
                    locPoint.dimType == Level.OVERWORLD ?
                        ChatFormatting.GREEN :
                        locPoint.dimType == Level.NETHER ?
                            ChatFormatting.RED :
                            locPoint.dimType == Level.END ?
                                ChatFormatting.LIGHT_PURPLE :
                                ChatFormatting.AQUA
                )
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(locPoint.dimType.location().toString())))
        );
        double scale = 0;
        if (locPoint.dimType == Level.NETHER) {
            scale = 8;
        } else if (locPoint.dimType == Level.OVERWORLD) {
            scale = 0.125;
        }
        MutableComponent toPos = Component.literal("[%.2f, %.2f, %.2f]".formatted(locPoint.x * scale, locPoint.y * scale, locPoint.z * scale)).withStyle(
            Style.EMPTY
                .applyFormat(
                    locPoint.dimType == Level.OVERWORLD ?
                        ChatFormatting.RED :
                        locPoint.dimType == Level.NETHER ?
                            ChatFormatting.GREEN :
                            ChatFormatting.AQUA
                )
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(locPoint.dimType.location().toString())))
        );
        MutableComponent addMap = Component.literal("[+X]").withStyle(
            Style.EMPTY
                .applyFormat(ChatFormatting.GREEN)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Add to Xaero's minimap")))
                .withClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/xaero_waypoint_add:%s:%s:%s:%s:%s:0:false:0:Internal_%s_waypoints".formatted(
                        locPoint.desc,
                        locPoint.desc.substring(0, 1),
                        (int) locPoint.x,
                        (int) locPoint.y,
                        (int) locPoint.z,
                        locPoint.dimType.location().getPath()
                    )
                ))
        );
        MutableComponent remove = Component.literal("[\uD83D\uDDD1]").withStyle(
            Style.EMPTY
                .applyFormat(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/loc remove %s".formatted(locPoint.id)))
        );
        MutableComponent component1 = Component.literal("▶ ").append(component)
            .append(" ").append(pos);
        if (scale > 0) component1.append("->").append(toPos);
        return component1
            .append(" ").append(addMap)
            .append(" ").append(remove);
    }

    public record LocPoint(
        long id,
        String desc,
        double x,
        double y,
        double z,
        @SerializedName("dim_type") ResourceKey<Level> dimType
    ) {
    }
}
