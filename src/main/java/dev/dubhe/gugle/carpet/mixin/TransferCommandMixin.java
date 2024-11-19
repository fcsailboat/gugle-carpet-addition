package dev.dubhe.gugle.carpet.mixin;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.commands.Commands;
//#if MC>=12100
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.dubhe.gugle.carpet.GcaSetting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.TransferCommand;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
//#else
//#endif

//#if MC>=12100
@Mixin(TransferCommand.class)
//#else
//$$ @Mixin(Commands.class)
//#endif
public class TransferCommandMixin {
    //#if MC>=12100
    @Redirect(method = "method_56524", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/CommandSourceStack;hasPermission(I)Z"))
    private static boolean registerPermission(CommandSourceStack instance, int i) {
        return GcaSetting.commandTransfer || instance.hasPermission(i);
    }

    @Contract(pure = true)
    @Redirect(method = "register", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/Commands;argument(Ljava/lang/String;Lcom/mojang/brigadier/arguments/ArgumentType;)Lcom/mojang/brigadier/builder/RequiredArgumentBuilder;", ordinal = 2))
    private static <T> @NotNull RequiredArgumentBuilder<CommandSourceStack, T> register(String string, ArgumentType<T> argumentType) {
        return Commands.argument(string, argumentType).requires(stack -> stack.hasPermission(3));
    }
    //#else
    //#endif
}
