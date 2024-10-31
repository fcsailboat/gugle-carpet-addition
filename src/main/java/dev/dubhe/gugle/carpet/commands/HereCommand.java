package dev.dubhe.gugle.carpet.commands;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.dubhe.gugle.carpet.GcaSetting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HereCommand {
    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("here")
                .requires(stack -> CommandHelper.canUseCommand(stack, GcaSetting.commandHere))
                .executes(HereCommand::execute)
        );
    }

    public static int execute(@NotNull CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) return 0;
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        server.getPlayerList().broadcastSystemMessage(HereCommand.playerPos(player), false);
        return 1;
    }

    public static @NotNull MutableComponent playerPos(@NotNull ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, true, false));
        Vec3 position = player.position();
        ResourceKey<Level> dimension = player.level().dimension();
        String name = player.getGameProfile().getName();
        List<MutableComponent> pos = LocCommand.pos("Shared Location", position.x, position.y, position.z, dimension);
        MutableComponent component = Component.literal("%s at".formatted(name)).append(" ").append(pos.get(0));
        if (pos.size() > 2) component.append("->").append(pos.get(2));
        return component;
    }
}
