package dev.dubhe.gugle.carpet.mixin;

import dev.dubhe.gugle.carpet.GcaSetting;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.StackedContents;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


//#if MC>=12100
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.crafting.RecipeHolder;
//#else
//$$ import org.spongepowered.asm.mixin.injection.Redirect;
//$$ import net.minecraft.world.item.crafting.Recipe;
//#endif

@Mixin(ServerPlaceRecipe.class)
abstract class ServerPlaceRecipeMixin {
    //#if MC>=12100
    @WrapOperation(method = "handleRecipeClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/StackedContents;getBiggestCraftableStack(Lnet/minecraft/world/item/crafting/RecipeHolder;Lit/unimi/dsi/fastutil/ints/IntList;)I"))
    private int handleRecipeClicked(StackedContents instance, RecipeHolder<?> recipeHolder, IntList intList, @NotNull Operation<Integer> original) {
        int i = original.call(instance, recipeHolder, intList);
        return GcaSetting.betterQuickCrafting ? i - 1 : i;
    }
    //#else
    //$$ @Redirect(method = "handleRecipeClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/StackedContents;getBiggestCraftableStack(Lnet/minecraft/world/item/crafting/Recipe;Lit/unimi/dsi/fastutil/ints/IntList;)I"))
    //$$ private int handleRecipeClicked(@NotNull StackedContents instance, Recipe<?> recipe, IntList intList) {
    //$$     int i = instance.getBiggestCraftableStack(recipe, intList);
    //$$     return GcaSetting.betterQuickCrafting ? i - 1 : i;
    //$$ }
    //#endif
}
