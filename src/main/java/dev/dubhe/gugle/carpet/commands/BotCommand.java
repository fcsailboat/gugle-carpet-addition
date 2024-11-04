package dev.dubhe.gugle.carpet.commands;

import carpet.fakes.ServerPlayerInterface;
import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
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
import dev.dubhe.gugle.carpet.tools.FilesUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BotCommand {
    public static final FilesUtil< String, BotInfo > BOT_INFO = new FilesUtil<>( "bot", Object::toString, BotInfo.class );
    public static final FilesUtil< String, BotGroupInfo > BOT_GROUP_INFO = new FilesUtil<>( "botGroup", Object::toString, BotGroupInfo.class );

    public static void register( @NotNull CommandDispatcher< CommandSourceStack > dispatcher ) {
        dispatcher.register( Commands.literal( "bot" )
                .requires( stack -> CommandHelper.canUseCommand( stack, GcaSetting.commandBot ) )
                .executes( BotCommand::list )
                .then(
                        Commands.literal( "list" ).executes( BotCommand::list )
                                .then(
                                        Commands.argument( "page", IntegerArgumentType.integer( 1 ) )
                                                .executes( BotCommand::list )
                                )
                )
                .then(
                        Commands.literal( "add" )
                                .then(
                                        Commands.argument( "player", EntityArgument.player() )
                                                .then(
                                                        Commands.argument( "desc", StringArgumentType.greedyString() )
                                                                .executes( BotCommand::add )
                                                )
                                )
                )
                .then(
                        Commands.literal( "load" )
                                .then(
                                        Commands.argument( "player", StringArgumentType.string() )
                                                .suggests( BotCommand::suggestPlayer )
                                                .executes( BotCommand::load )
                                )
                )
                .then(
                        Commands.literal( "remove" )
                                .then(
                                        Commands.argument( "player", StringArgumentType.string() )
                                                .suggests( BotCommand::suggestPlayer )
                                                .executes( BotCommand::remove )
                                )
                )
                .then(
                        Commands.literal( "group" )
                                .then(
                                        Commands.literal( "create" )
                                                .then(
                                                        Commands.argument( "name", StringArgumentType.string() )
                                                                .executes( BotCommand::groupCreate )
                                                )
                                )
                                .then(
                                        Commands.literal( "list" ).executes( BotCommand::groupList )
                                                .then(
                                                        Commands.argument( "page", IntegerArgumentType.integer( 1 ) )
                                                                .executes( BotCommand::groupList )
                                                )
                                )
                                .then(
                                        Commands.literal( "remove" )
                                                .then(
                                                        Commands.argument( "name", StringArgumentType.string() )
                                                                .executes( BotCommand::groupRemove )
                                                )
                                )
                                .then(
                                        Commands.literal( "addBot" )
                                                .then(
                                                        Commands.argument( "groupName", StringArgumentType.string() )
                                                                .suggests( BotCommand::suggestGroup )
                                                                .then(
                                                                        Commands.argument( "botName", StringArgumentType.string() )
                                                                                .suggests( BotCommand::suggestPlayer )
                                                                                .executes( BotCommand::groupAddBot )
                                                                )
                                                )
                                )
                                .then(
                                        Commands.literal( "removeBot" )
                                                .then(
                                                        Commands.argument( "groupName", StringArgumentType.string() )
                                                                .suggests( BotCommand::suggestGroup )
                                                                .then(
                                                                        Commands.argument( "botName", StringArgumentType.string() )
                                                                                .suggests( BotCommand::suggestPlayer )
                                                                                .executes( BotCommand::groupRemoveBot )
                                                                )
                                                )
                                )
                                .then(
                                        Commands.literal( "loadGroup" )
                                                .then(
                                                        Commands.argument( "groupName", StringArgumentType.string() )
                                                                .executes( BotCommand::groupLoadBot )
                                                )
                                )
                                .then(
                                        Commands.literal( "unloadGroup" )
                                                .then(
                                                        Commands.argument( "groupName", StringArgumentType.string() )
                                                                .executes( BotCommand::groupUnloadBot )
                                                )
                                )
                                .then(
                                        Commands.literal( "show" )
                                                .then(
                                                        Commands.argument( "groupName", StringArgumentType.string() )
                                                                .executes( BotCommand::groupShow )
                                                )
                                )
                )
        );
    }

    private static int groupShow( CommandContext< CommandSourceStack > context ) throws CommandSyntaxException {
        BOT_GROUP_INFO.init( context );
        BOT_INFO.init( context );
        CommandSourceStack source = context.getSource();
        String groupName = StringArgumentType.getString( context, "groupName" );
        if ( ! BOT_GROUP_INFO.map.containsKey( groupName ) ) {
            source.sendFailure( Component.literal( "Group %s is not found.".formatted( groupName ) ) );
            return 0;
        }
        List< String > botNames = BOT_GROUP_INFO.map.get( groupName ).bots;
        List< String > failedBots = new ArrayList<>();
        for ( String botName : botNames ) {
            if ( ! BOT_INFO.map.containsKey( botName ) ) {
                failedBots.add( botName );
            }
        }
        botNames.removeAll( failedBots );
        BOT_GROUP_INFO.map.put(
                groupName,
                new BotGroupInfo( groupName, botNames )
        );
        BOT_GROUP_INFO.save();
        int page;
        try {
            page = IntegerArgumentType.getInteger( context, "page" );
        } catch ( IllegalArgumentException ignored ) {
            page = 1;
        }
        final int pageSize = 8;
        int size = BOT_GROUP_INFO.map.get( groupName ).bots.size();
        int maxPage = size / pageSize + 1;
        if ( page > maxPage ) {
            context.getSource().sendFailure( Component.literal( "No such page %s".formatted( page ) ) );
            return 0;
        }
        ArrayList< BotInfo > botInfos = new ArrayList<>();
        for ( String botName : BOT_GROUP_INFO.map.get( groupName ).bots ) {
            botInfos.add( BOT_INFO.map.get( botName ) );
        }
        context.getSource().sendSystemMessage(
                Component.literal( "======= Bot List (Page %s/%s) =======".formatted( page, maxPage ) )
                        .withStyle( ChatFormatting.YELLOW )
        );
        for ( int i = (page - 1) * pageSize; i < size && i < page * pageSize; i++ ) {
            context.getSource().sendSystemMessage( botToComponent( botInfos.get( i ) ) );
        }
        listComponent( context, page, maxPage, "/bot group show" );
        return 1;
    }

    private static int groupUnloadBot( CommandContext< CommandSourceStack > context ) throws CommandSyntaxException {
        BOT_GROUP_INFO.init( context );
        BOT_INFO.init( context );
        CommandSourceStack source = context.getSource();
        String groupName = StringArgumentType.getString( context, "groupName" );
        if ( ! BOT_GROUP_INFO.map.containsKey( groupName ) ) {
            source.sendFailure( Component.literal( "Group %s is not found.".formatted( groupName ) ) );
            return 0;
        }
        List< String > botNames = BOT_GROUP_INFO.map.get( groupName ).bots;
        List< String > failedBots = new ArrayList<>();
        for ( String botName : botNames ) {
            if ( ! BOT_INFO.map.containsKey( botName ) ) {
                failedBots.add( botName );
                continue;
            }
            source.getServer().getCommands().performPrefixedCommand( source, "/player %s kill".formatted( botName ) );
        }
        botNames.removeAll( failedBots );
        BOT_GROUP_INFO.map.put(
                groupName,
                new BotGroupInfo( groupName, botNames )
        );
        BOT_GROUP_INFO.save();
        return 1;
    }

    private static int groupLoadBot( CommandContext< CommandSourceStack > context ) throws CommandSyntaxException {
        BOT_GROUP_INFO.init( context );
        BOT_INFO.init( context );
        CommandSourceStack source = context.getSource();
        String groupName = StringArgumentType.getString( context, "groupName" );
        if ( ! BOT_GROUP_INFO.map.containsKey( groupName ) ) {
            source.sendFailure( Component.literal( "Group %s is not found.".formatted( groupName ) ) );
            return 0;
        }
        List< String > botNames = BOT_GROUP_INFO.map.get( groupName ).bots;
        List< String > failedBots = new ArrayList<>();
        for ( String botName : new ArrayList<>( botNames ) ) {
            if ( ! BOT_INFO.map.containsKey( botName ) ) {
                failedBots.add( botName );
                continue;
            }
            BotInfo botInfo = BOT_INFO.map.get( botName );
            boolean success = EntityPlayerMPFake.createFake(
                    botName,
                    BOT_INFO.server,
                    botInfo.pos,
                    botInfo.facing.y,
                    botInfo.facing.x,
                    botInfo.dimType,
                    botInfo.mode,
                    botInfo.flying
            );
            if ( success ) {
                if ( botInfo.actions != null ) {
                    GcaExtension.ON_PLAYER_LOGGED_IN.put(
                            botName,
                            ( player ) -> FakePlayerSerializer.applyActionPackFromJson( botInfo.actions, player )
                    );
                }
            } else {
                source.sendFailure( Component.literal( "%s is not loaded.".formatted( botName ) ) );
                failedBots.add( botName );
            }
        }
        botNames.removeAll( failedBots );
        BOT_GROUP_INFO.map.put(
                groupName,
                new BotGroupInfo( groupName, botNames )
        );
        BOT_GROUP_INFO.save();
        return 1;
    }

    private static int groupRemoveBot( CommandContext< CommandSourceStack > context ) throws CommandSyntaxException {
        BOT_GROUP_INFO.init( context );
        CommandSourceStack source = context.getSource();
        String groupName = StringArgumentType.getString( context, "groupName" );
        String botName = StringArgumentType.getString( context, "botName" );
        if ( ! BOT_GROUP_INFO.map.containsKey( groupName ) ) {
            source.sendFailure( Component.literal( "Group %s is not found.".formatted( groupName ) ) );
            return 0;
        }
        List< String > botNames = BOT_GROUP_INFO.map.get( groupName ).bots;
        if ( ! botNames.contains( botName ) ) {
            source.sendFailure( Component.literal( "Bot %s is not found in the %s.".formatted( botName, groupName ) ) );
            return 0;
        }
        botNames.remove( botName );
        BotCommand.BOT_GROUP_INFO.map.put(
                groupName,
                new BotGroupInfo(
                        groupName,
                        botNames
                )
        );
        BOT_GROUP_INFO.save();
        source.sendSuccess( () -> Component.literal( "Bot %s is removed from %s successfully.".formatted( botName, groupName ) ), false );
        return 1;
    }

    private static int groupAddBot( CommandContext< CommandSourceStack > context ) throws CommandSyntaxException {
        BOT_GROUP_INFO.init( context );
        BOT_INFO.init( context );
        CommandSourceStack source = context.getSource();
        String groupName = StringArgumentType.getString( context, "groupName" );
        String botName = StringArgumentType.getString( context, "botName" );
        if ( ! BOT_GROUP_INFO.map.containsKey( groupName ) ) {
            source.sendFailure( Component.literal( "Group %s is not found.".formatted( groupName ) ) );
            return 0;
        }
        if ( ! BOT_INFO.map.containsKey( botName ) ) {
            source.sendFailure( Component.literal( "Bot %s is not found.".formatted( botName ) ) );
            return 0;
        }
        List< String > botNames = BOT_GROUP_INFO.map.get( groupName ).bots;
        if ( botNames.contains( botName ) ) {
            source.sendFailure( Component.literal( "Bot %s is already added.".formatted( botName ) ) );
            return 0;
        }
        botNames.add( botName );
        BotCommand.BOT_GROUP_INFO.map.put(
                groupName,
                new BotGroupInfo(
                        groupName,
                        botNames
                )
        );
        BOT_GROUP_INFO.save();
        source.sendSuccess( () -> Component.literal( "Bot %s is added to %s successfully.".formatted( botName, groupName ) ), false );
        return 1;
    }

    private static int groupCreate( CommandContext< CommandSourceStack > context ) throws CommandSyntaxException {
        BOT_GROUP_INFO.init( context );
        CommandSourceStack source = context.getSource();
        String groupName = StringArgumentType.getString( context, "name" );
        if ( BOT_GROUP_INFO.map.containsKey( groupName ) ) {
            source.sendFailure( Component.literal( "Group %s already exists.".formatted( groupName ) ) );
            return 0;
        }
        BOT_GROUP_INFO.map.put(
                groupName,
                new BotGroupInfo( groupName, new ArrayList<>() )
        );
        BOT_GROUP_INFO.save();
        source.sendSuccess( () -> Component.literal( "Group %s created successfully.".formatted( groupName ) ), false );
        return 1;
    }

    private static int groupList( CommandContext< CommandSourceStack > context ) {
        BOT_GROUP_INFO.init( context );
        int page;
        try {
            page = IntegerArgumentType.getInteger( context, "page" );
        } catch ( IllegalArgumentException ignored ) {
            page = 1;
        }
        final int pageSize = 8;
        int size = BOT_GROUP_INFO.map.size();
        int maxPage = size / pageSize + 1;
        if ( page > maxPage ) {
            context.getSource().sendFailure( Component.literal( "No such page %s".formatted( page ) ) );
            return 0;
        }
        BotGroupInfo[] botGroupInfos = BOT_GROUP_INFO.map.values().toArray( new BotGroupInfo[0] );
        context.getSource().sendSystemMessage(
                Component.literal( "======= Bot Group List (Page %s/%s) =======".formatted( page, maxPage ) )
                        .withStyle( ChatFormatting.YELLOW )
        );
        for ( int i = (page - 1) * pageSize; i < size && i < page * pageSize; i++ ) {
            context.getSource().sendSystemMessage( botGroupToComponent( botGroupInfos[i] ) );
        }
        listComponent( context, page, maxPage, "/bot group list" );
        return 1;
    }

    private static int list( CommandContext< CommandSourceStack > context ) {
        BOT_INFO.init( context );
        int page;
        try {
            page = IntegerArgumentType.getInteger( context, "page" );
        } catch ( IllegalArgumentException ignored ) {
            page = 1;
        }
        final int pageSize = 8;
        int size = BOT_INFO.map.size();
        int maxPage = size / pageSize + 1;
        if ( page > maxPage ) {
            context.getSource().sendFailure( Component.literal( "No such page %s".formatted( page ) ) );
            return 0;
        }
        BotInfo[] botInfos = BOT_INFO.map.values().toArray( new BotInfo[0] );
        context.getSource().sendSystemMessage(
                Component.literal( "======= Bot List (Page %s/%s) =======".formatted( page, maxPage ) )
                        .withStyle( ChatFormatting.YELLOW )
        );
        for ( int i = (page - 1) * pageSize; i < size && i < page * pageSize; i++ ) {
            context.getSource().sendSystemMessage( botToComponent( botInfos[i] ) );
        }
        listComponent( context, page, maxPage, "/bot list" );
        return 1;
    }

    private static @NotNull MutableComponent botGroupToComponent( @NotNull BotGroupInfo botGroupInfo ) {
        MutableComponent name = Component.literal( botGroupInfo.name ).withStyle(
                Style.EMPTY
                        .applyFormat( ChatFormatting.GRAY )
                        .withHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, Component.literal( botGroupInfo.name ) ) )
        );
        MutableComponent load = Component.literal( "[↑]" ).withStyle(
                Style.EMPTY
                        .applyFormat( ChatFormatting.GREEN )
                        .withHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, Component.literal( "Load Group" ) ) )
                        .withClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/bot group loadGroup %s".formatted( botGroupInfo.name ) ) )
        );
        MutableComponent remove = Component.literal( "[↓]" ).withStyle(
                Style.EMPTY
                        .applyFormat( ChatFormatting.RED )
                        .withHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, Component.literal( "Unload Group" ) ) )
                        .withClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/bot group unloadGroup %s".formatted( botGroupInfo.name ) ) )
        );
        MutableComponent delete = Component.literal( "[\uD83D\uDDD1]" ).withStyle(
                Style.EMPTY
                        .applyFormat( ChatFormatting.RED )
                        .withHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, Component.literal( "Remove Bot Group" ) ) )
                        .withClickEvent( new ClickEvent( ClickEvent.Action.SUGGEST_COMMAND, "/bot group remove %s".formatted( botGroupInfo.name ) ) )
        );
        MutableComponent component = Component.literal( "▶ " ).append( name );
        component.append( " " ).append( load );
        component.append( " " ).append( remove );
        return component.append( " " ).append( delete );
    }

    private static @NotNull MutableComponent botToComponent( @NotNull BotInfo botInfo ) {
        MutableComponent desc = Component.literal( botInfo.desc ).withStyle(
                Style.EMPTY
                        .applyFormat( ChatFormatting.GRAY )
                        .withHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, Component.literal( botInfo.name ) ) )
        );
        MutableComponent load = Component.literal( "[↑]" ).withStyle(
                Style.EMPTY
                        .applyFormat( ChatFormatting.GREEN )
                        .withHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, Component.literal( "Load bot" ) ) )
                        .withClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/bot load %s".formatted( botInfo.name ) ) )
        );
        MutableComponent remove = Component.literal( "[↓]" ).withStyle(
                Style.EMPTY
                        .applyFormat( ChatFormatting.RED )
                        .withHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, Component.literal( "Unload bot" ) ) )
                        .withClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/player %s kill".formatted( botInfo.name ) ) )
        );
        MutableComponent delete = Component.literal( "[\uD83D\uDDD1]" ).withStyle(
                Style.EMPTY
                        .applyFormat( ChatFormatting.RED )
                        .withHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, Component.literal( "Remove bot" ) ) )
                        .withClickEvent( new ClickEvent( ClickEvent.Action.SUGGEST_COMMAND, "/bot remove %s".formatted( botInfo.name ) ) )
        );
        boolean notOnline = BOT_INFO.server.getPlayerList().getPlayerByName( botInfo.name ) == null;
        MutableComponent component = Component.literal( "▶ " )
                .withStyle( notOnline ? ChatFormatting.RED : ChatFormatting.GREEN )
                .append( desc );
        if ( notOnline ) {
            component.append( " " ).append( load );
        } else {
            component.append( " " ).append( remove );
        }
        return component.append( " " ).append( delete );
    }

    private static int load( CommandContext< CommandSourceStack > context ) {
        BOT_INFO.init( context );
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString( context, "player" );
        if ( BOT_INFO.server.getPlayerList().getPlayerByName( name ) != null ) {
            source.sendFailure( Component.literal( "player %s is already exist.".formatted( name ) ) );
            return 0;
        }
        BotInfo botInfo = BOT_INFO.map.getOrDefault( name, null );
        if ( botInfo == null ) {
            source.sendFailure( Component.literal( "%s is not exist." ) );
            return 0;
        }
        boolean success = EntityPlayerMPFake.createFake(
                name,
                BOT_INFO.server,
                botInfo.pos,
                botInfo.facing.y,
                botInfo.facing.x,
                botInfo.dimType,
                botInfo.mode,
                botInfo.flying
        );
        if ( success ) {
            if ( botInfo.actions != null ) {
                GcaExtension.ON_PLAYER_LOGGED_IN.put(
                        name,
                        ( player ) -> FakePlayerSerializer.applyActionPackFromJson( botInfo.actions, player )
                );
            }
            return 1;
        } else {
            source.sendFailure( Component.literal( "%s is not loaded.".formatted( name ) ) );
            return 0;
        }
    }

    private static int add( CommandContext< CommandSourceStack > context ) throws CommandSyntaxException {
        BOT_INFO.init( context );
        CommandSourceStack source = context.getSource();
        ServerPlayer p;
        if ( ! ((p = EntityArgument.getPlayer( context, "player" )) instanceof EntityPlayerMPFake player) ) {
            source.sendFailure( Component.literal( "%s is not a fake player.".formatted( p.getGameProfile().getName() ) ) );
            return 0;
        }
        String name = player.getGameProfile().getName();
        if ( BOT_INFO.map.containsKey( name ) ) {
            source.sendFailure( Component.literal( "%s is already save.".formatted( name ) ) );
            return 0;
        }
        BotCommand.BOT_INFO.map.put(
                name,
                new BotInfo(
                        name,
                        StringArgumentType.getString( context, "desc" ),
                        player.position(),
                        player.getRotationVector(),
                        player.level().dimension(),
                        player.gameMode.getGameModeForPlayer(),
                        player.getAbilities().flying,
                        FakePlayerSerializer.actionPackToJson( (( ServerPlayerInterface ) player).getActionPack() )
                )
        );
        BOT_INFO.save();
        source.sendSuccess( () -> Component.literal( "%s is added.".formatted( name ) ), false );
        return 1;
    }

    private static int groupRemove( CommandContext< CommandSourceStack > context ) {
        BOT_GROUP_INFO.init( context );
        String name = StringArgumentType.getString( context, "name" );
        BotGroupInfo remove = BotCommand.BOT_GROUP_INFO.map.remove( name );
        if ( remove == null ) {
            context.getSource().sendFailure( Component.literal( "Bot Group %s is not exist.".formatted( name ) ) );
            return 0;
        }
        context.getSource().sendSuccess( () -> Component.literal( "%s is removed.".formatted( name ) ), false );
        BOT_GROUP_INFO.save();
        return 1;
    }

    private static int remove( CommandContext< CommandSourceStack > context ) {
        BOT_INFO.init( context );
        String name = StringArgumentType.getString( context, "player" );
        BotInfo remove = BotCommand.BOT_INFO.map.remove( name );
        if ( remove == null ) {
            context.getSource().sendFailure( Component.literal( "Bot %s is not exist.".formatted( name ) ) );
            return 0;
        }
        context.getSource().sendSuccess( () -> Component.literal( "%s is removed.".formatted( name ) ), false );
        BOT_INFO.save();
        return 1;
    }

    private static void listComponent( CommandContext< CommandSourceStack > context, int page, int maxPage, String command ) {
        Component prevPage = page <= 1 ?
                Component.literal( "<<<" ).withStyle( ChatFormatting.GRAY ) :
                Component.literal( "<<<" ).withStyle(
                        Style.EMPTY
                                .applyFormat( ChatFormatting.GREEN )
                                .withClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, command + (page - 1) ) )
                );
        Component nextPage = page >= maxPage ?
                Component.literal( ">>>" ).withStyle( ChatFormatting.GRAY ) :
                Component.literal( ">>>" ).withStyle(
                        Style.EMPTY
                                .applyFormat( ChatFormatting.GREEN )
                                .withClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, command + (page + 1) ) )
                );
        context.getSource().sendSystemMessage(
                Component.literal( "=======" )
                        .withStyle( ChatFormatting.YELLOW )
                        .append( " " )
                        .append( prevPage )
                        .append( " " )
                        .append( Component.literal( "(Page %s/%s)".formatted( page, maxPage ) ).withStyle( ChatFormatting.YELLOW ) )
                        .append( " " )
                        .append( nextPage )
                        .append( " " )
                        .append( Component.literal( "=======" ).withStyle( ChatFormatting.YELLOW ) )
        );
    }

    private static @NotNull CompletableFuture< Suggestions > suggestPlayer( final CommandContext< CommandSourceStack > context, final SuggestionsBuilder builder ) {
        return SharedSuggestionProvider.suggest( BOT_INFO.map.keySet(), builder );
    }

    private static @NotNull CompletableFuture< Suggestions > suggestGroup( final CommandContext< CommandSourceStack > context, final SuggestionsBuilder builder ) {
        return SharedSuggestionProvider.suggest( BOT_GROUP_INFO.map.keySet(), builder );
    }

    public record BotInfo(
            String name,
            String desc,
            Vec3 pos,
            Vec2 facing,
            @SerializedName("dim_type") ResourceKey< Level > dimType,
            GameType mode,
            boolean flying,
            JsonObject actions
    ) {
    }

    public record BotGroupInfo(
            String name,
            List< String > bots
    ) {
    }
}
