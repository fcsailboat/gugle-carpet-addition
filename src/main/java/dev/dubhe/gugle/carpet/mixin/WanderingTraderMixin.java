package dev.dubhe.gugle.carpet.mixin;

import dev.dubhe.gugle.carpet.GcaSetting;
import dev.dubhe.gugle.carpet.api.tools.text.Color;
import dev.dubhe.gugle.carpet.api.tools.text.ComponentTranslate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.WanderingTraderSpawner;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


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
            this.gca$sendMsg(
                ComponentTranslate.trans(
                    "carpet.rule.wanderingTraderSpawnFailedWarning.tip.02",
                    Color.YELLOW,
                    Style.EMPTY,
                    "i <= %s".formatted(this.spawnChance),
                    result
                )
            );
        }
        return result;
    }

    @Redirect(method = "spawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int spawn1(@NotNull RandomSource instance, int i) {
        int result = instance.nextInt(i);
        if (result != 0) {
            this.gca$sendMsg(
                ComponentTranslate.trans(
                    "carpet.rule.wanderingTraderSpawnFailedWarning.tip.02",
                    Color.YELLOW,
                    Style.EMPTY,
                    "i == 0",
                    result
                )
            );
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
        this.gca$sendMsg(
            ComponentTranslate.trans(
                "carpet.rule.wanderingTraderSpawnFailedWarning.tip.03",
                Color.YELLOW,
                Style.EMPTY,
                this.gca$player.getDisplayName()
            )
        );
    }

    @Inject(method = "spawn", at = @At(value = "RETURN", ordinal = 4))
    private void spawn3(ServerLevel serverLevel, CallbackInfoReturnable<Boolean> cir) {
        this.gca$sendMsg(
            ComponentTranslate.trans(
                "carpet.rule.wanderingTraderSpawnFailedWarning.tip.04",
                Color.YELLOW,
                Style.EMPTY,
                this.gca$player.getDisplayName()
            )
        );
    }

    @Unique
    private void gca$sendMsg(Component msg) {
        if (!GcaSetting.wanderingTraderSpawnFailedWarning) return;
        if (this.gca$server == null) return;
        this.gca$server.getPlayerList().broadcastSystemMessage(
            ComponentTranslate.trans(
                "carpet.rule.wanderingTraderSpawnFailedWarning.tip.01",
                Color.YELLOW,
                Style.EMPTY
            ),
            false
        );
        this.gca$server.getPlayerList().broadcastSystemMessage(msg, false);
    }
}
