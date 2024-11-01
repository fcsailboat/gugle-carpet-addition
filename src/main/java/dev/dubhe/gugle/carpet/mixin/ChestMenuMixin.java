package dev.dubhe.gugle.carpet.mixin;

import dev.dubhe.gugle.carpet.api.menu.control.Button;
import dev.dubhe.gugle.carpet.tools.FakePlayerInventoryMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestMenu.class)
public abstract class ChestMenuMixin {
    @Unique
    private final ChestMenu thisMenu = (ChestMenu) (Object) this;

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void quickMove(Player player, int i, CallbackInfoReturnable<ItemStack> cir) {
        if (this.isFakePlayerMenu()) {
            cir.setReturnValue(FakePlayerInventoryMenu.quickMove(thisMenu, i));
        }
    }

    @Unique
    private boolean isFakePlayerMenu() {
        ItemStack itemStack = thisMenu.getSlot(0).getItem();
        if (itemStack.is(Items.STRUCTURE_VOID)) {
            CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
            return customData != null && customData.copyTag().get(Button.GCA_CLEAR) != null;
        }
        return false;
    }
}
