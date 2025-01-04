package dev.dubhe.gugle.carpet.mixin;

import dev.dubhe.gugle.carpet.GcaSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.WanderingTraderSpawner;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Debug(export = true, print = true)
@Mixin(WanderingTraderSpawner.class)
abstract class WanderingTraderMixin {
    @Unique
    private MinecraftServer gca$server = null;
    @Unique
    private ServerPlayer gca$player = null;
    @Shadow
    private int spawnChance;

    @Inject(method = "tick", at = @At("HEAD"))
    private void spawn(@NotNull ServerLevel serverLevel, boolean bl, boolean bl2, CallbackInfoReturnable<Integer> cir) {
        this.gca$server = serverLevel.getServer();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int spawn0(@NotNull RandomSource instance, int i) {
        int result = instance.nextInt(i);
        if (result > this.spawnChance) {
            this.gca$sendMsg("Probability not met, expected i > %s, but obtained %s".formatted(this.spawnChance, result));
        }
        return result;
    }

    @Redirect(method = "spawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int spawn1(@NotNull RandomSource instance, int i) {
        int result = instance.nextInt(i);
        if (result != 0) {
            this.gca$sendMsg("Probability not met, expected i != 0, but obtained %s".formatted(result));
        }
        return result;
    }

    @Contract(pure = true)
    @Redirect(method = "spawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getRandomPlayer()Lnet/minecraft/server/level/ServerPlayer;"))
    private @Nullable ServerPlayer getRandomPlayer(@NotNull ServerLevel instance) {
        this.gca$player = instance.getRandomPlayer();
        return this.gca$player;
    }

    @Inject(method = "spawn", at = @At(value = "RETURN", ordinal = 2))
    private void spawn2(ServerLevel serverLevel, CallbackInfoReturnable<Boolean> cir) {
        this.gca$sendMsg("The biome does not meet the requirements, Current player: %s".formatted(this.gca$player.getScoreboardName()));
    }

    @Inject(method = "spawn", at = @At(value = "RETURN", ordinal = 4))
    private void spawn3(ServerLevel serverLevel, CallbackInfoReturnable<Boolean> cir) {
        this.gca$sendMsg("Not has enough space, Current player: %s".formatted(this.gca$player.getScoreboardName()));
    }

    @Unique
    private void gca$sendMsg(String msg) {
        if (!GcaSetting.wanderingTraderSpawnFailedWarning) return;
        if (this.gca$server == null) return;
        this.gca$server.getPlayerList().broadcastSystemMessage(
            Component.literal("Wandering Trader Spawn Failed, Reason: ").withStyle(ChatFormatting.YELLOW),
            false
        );
        this.gca$server.getPlayerList().broadcastSystemMessage(
            Component.literal(msg).withStyle(ChatFormatting.YELLOW),
            false
        );
    }
}
