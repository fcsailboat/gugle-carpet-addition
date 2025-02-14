package dev.dubhe.gugle.carpet.tools;

import dev.dubhe.gugle.carpet.mixin.AbstractContainerMenuAccessor;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

//#if MC>=12104
//$$ import net.minecraft.world.item.equipment.Equippable;
//$$ import net.minecraft.world.entity.EquipmentSlot;
//#endif
//#if MC>=12100
import net.minecraft.core.component.DataComponents;
//#else
//#endif

public class FakePlayerInventoryMenu extends ChestMenu {
    public FakePlayerInventoryMenu(int i, Inventory inventory, Container container) {
        super(MenuType.GENERIC_9x6, i, inventory, container, 6);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        return quickMove(this, slotIndex);
    }

    public static ItemStack quickMove(ChestMenu chestMenu, int slotIndex) {
        ItemStack remainingItem = ItemStack.EMPTY;
        Slot slot = chestMenu.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            remainingItem = slotStack.copy();
            if (slotIndex < 54) {
                AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) (chestMenu);
                if (!accessor.invokerMoveItemStackTo(slotStack, 54, chestMenu.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotStack.getItem() instanceof ArmorItem armorItem) {
                // 如果是盔甲，移动到盔甲槽
                int ordinal = getArmorOrdinal(armorItem);
                if (ordinal >= 0 && FakePlayerInventoryMenu.moveToArmor(chestMenu, slotStack, ordinal) || moveToInventory(chestMenu, slotStack)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotStack.is(Items.ELYTRA)) {
                // 如果是鞘翅，移动到盔甲槽
                if (FakePlayerInventoryMenu.moveToArmor(chestMenu, slotStack, 1) || moveToInventory(chestMenu, slotStack)) {
                    return ItemStack.EMPTY;
                }
            } else if (
                //#if MC>=12100
                slotStack.has(DataComponents.FOOD)
                //#else
                //$$ slotStack.getItem().isEdible()
                //#endif
            ) {
                // 如果是食物，移动到副手
                if (FakePlayerInventoryMenu.moveToOffHand(chestMenu, slotStack) || moveToInventory(chestMenu, slotStack)) {
                    return ItemStack.EMPTY;
                }
            } else if (moveToInventory(chestMenu, slotStack)) {
                // 物品栏没有剩余空间了，移动到盔甲和副手
                AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) (chestMenu);
                if (accessor.invokerMoveItemStackTo(slotStack, 1, 8, false)) {
                    return ItemStack.EMPTY;
                }
                // 其它物品移动的物品栏中
                return ItemStack.EMPTY;
            }
            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return remainingItem;
    }

    private static int getArmorOrdinal(@NotNull ArmorItem armorItem) {
        int ordinal;
        //#if MC>=12104
        //$$ ordinal = -1;
        //$$ Equippable equippable = armorItem.components().get(DataComponents.EQUIPPABLE);
        //$$ if (equippable != null) {
        //$$     EquipmentSlot slot1 = equippable.slot();
        //$$     ordinal = switch (slot1) {
        //$$         case HEAD -> 0;
        //$$         case CHEST -> 1;
        //$$         case LEGS -> 2;
        //$$         case FEET -> 3;
        //$$         default -> -1;
        //$$     };
        //$$ }
        //#else
        ordinal = armorItem.getType().ordinal();
        //#endif
        return ordinal;
    }

    // 移动到副手
    private static boolean moveToOffHand(ChestMenu chestMenu, ItemStack slotStack) {
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) (chestMenu);
        return accessor.invokerMoveItemStackTo(slotStack, 7, 8, false);
    }

    // 移动到盔甲槽
    private static boolean moveToArmor(ChestMenu chestMenu, ItemStack slotStack, int ordinal) {
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) (chestMenu);
        return accessor.invokerMoveItemStackTo(slotStack, ordinal + 1, ordinal + 2, false);
    }

    // 将物品移动的物品栏
    private static boolean moveToInventory(ChestMenu chestMenu, ItemStack slotStack) {
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) (chestMenu);
        return !accessor.invokerMoveItemStackTo(slotStack, 18, 54, false);
    }
}
