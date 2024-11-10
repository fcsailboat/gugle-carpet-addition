package dev.dubhe.gugle.carpet.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.nfunk.jep.JEP;

public class SimpleInGameCalculator {
    public static void handleChat(@NotNull MinecraftServer server, @NotNull ServerPlayer player, @NotNull String msg) {
        server.getPlayerList().broadcastSystemMessage(SimpleInGameCalculator.calculate(msg), false);
    }

    public static @NotNull Component calculate(@NotNull String expression) {
        if (expression.startsWith("==")) expression = expression.substring(2);
        JEP jep = new JEP();
        // 添加常用函数
        jep.addStandardFunctions();
        // 添加常用常量
        jep.addStandardConstants();
        // 添加虚数
        jep.addComplex();
        if (!jep.hasError()) {
            jep.parseExpression(expression);
            double result = jep.getValue();
            return Component.literal("=%f".formatted(result)).withStyle(ChatFormatting.DARK_GRAY);
        } else {
            return Component.literal("Illegal expression");
        }
    }
}
