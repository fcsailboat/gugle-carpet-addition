package dev.dubhe.gugle.carpet.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.nfunk.jep.JEP;

public class SimpleInGameCalculator {
    public static @NotNull Component calculate(@NotNull String expression) {
        if (expression.startsWith("==")) expression = expression.substring(2);
        JEP jep = new JEP();
        // 添加常用函数
        jep.addStandardFunctions();
        // 添加常用常量
        jep.addStandardConstants();
        System.out.println(expression);
        try {
            jep.parseExpression(expression);
            double result = jep.getValue();
            return Component.literal("=%f".formatted(result)).withStyle(ChatFormatting.DARK_GRAY);
        } catch (Exception e) {
            return Component.literal("Illegal expression");
        }
    }
}
