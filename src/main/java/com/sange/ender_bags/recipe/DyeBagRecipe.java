package com.sange.ender_bags.recipe;

import com.sange.ender_bags.item.BagContents;
import com.sange.ender_bags.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class DyeBagRecipe extends CustomRecipe {
    public DyeBagRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, @NotNull Level level) {
        int bagCount = 0;
        int dyeCount = 0;

        for (int index = 0; index < input.size(); index++) {
            ItemStack stack = input.getItem(index);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModItems.ENDER_BAG.get())) {
                bagCount++;
            } else if (stack.getItem() instanceof DyeItem) {
                dyeCount++;
            } else {
                return false;
            }
        }
        return bagCount == 1 && dyeCount == 1;
    }

    @Override
    public @NotNull ItemStack assemble(
            CraftingInput input,
            HolderLookup.Provider registries) {
        ItemStack bag = ItemStack.EMPTY;
        DyeColor color = null;

        for (int index = 0; index < input.size(); index++) {
            ItemStack stack = input.getItem(index);
            if (stack.is(ModItems.ENDER_BAG.get())) {
                if (!bag.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                bag = stack;
            } else if (!stack.isEmpty() && stack.getItem() instanceof DyeItem dyeItem) {
                if (color != null) {
                    return ItemStack.EMPTY;
                }
                color = dyeItem.getDyeColor();
            } else if (!stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        if (bag.isEmpty() || color == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = bag.copyWithCount(1);
        BagContents.migrateLegacyData(result, registries);
        if (color == DyeColor.WHITE) {
            result.remove(DataComponents.DYED_COLOR);
        } else {
            result.set(DataComponents.DYED_COLOR,
                    new DyedItemColor(color.getTextureDiffuseColor() & 0xFFFFFF, false));
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.DYE_BAG_RECIPE.get();
    }
}
