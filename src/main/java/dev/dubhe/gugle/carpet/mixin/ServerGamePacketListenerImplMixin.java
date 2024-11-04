package dev.dubhe.gugle.carpet.mixin;

import dev.dubhe.gugle.carpet.GcaSetting;
import dev.dubhe.gugle.carpet.tools.SimpleInGameCalculator;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public abstract ServerPlayer getPlayer();

//    @Inject(method = "tryHandleChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;execute(Ljava/lang/Runnable;)V", shift = At.Shift.AFTER))
//    private void handleChat(String string, Runnable runnable, CallbackInfo ci) {
//        if (!GcaSetting.simpleInGameCalculator) return;
//        if (!string.startsWith("==") || string.startsWith("===")) return;
//        this.getPlayer().server.getPlayerList().broadcastSystemMessage(SimpleInGameCalculator.calculate(string), false);
//    }

    @Inject(method = "method_45064", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;)V", shift = At.Shift.AFTER))
    private void handleChatA(PlayerChatMessage playerChatMessage, Component component, FilteredText filteredText, CallbackInfo ci) {
        if (!GcaSetting.simpleInGameCalculator) return;
        String string = component.getString();
        if (!string.startsWith("==") || string.startsWith("===")) return;
        this.getPlayer().server.getPlayerList().broadcastSystemMessage(SimpleInGameCalculator.calculate(string), false);
    }
}
