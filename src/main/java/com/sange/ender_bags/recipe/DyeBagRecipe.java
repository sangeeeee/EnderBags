
package com.sange.ender_bags.recipe;

import com.google.gson.JsonObject;
import com.sange.ender_bags.item.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("removal")
public class DyeBagRecipe extends ShapelessRecipe {
    public DyeBagRecipe(ResourceLocation id, String group, ItemStack result, NonNullList<Ingredient> ingredients) {
        super(id, group, CraftingBookCategory.MISC, result, ingredients);
    }

    @Override
    public boolean matches(CraftingContainer inv, @NotNull Level level) {
        int bagCount = 0;
        int dyeCount = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModItems.ENDER_BAG.get())) {
                    bagCount++;
                } else if (stack.getItem() instanceof DyeItem) {
                    dyeCount++;
                } else {
                    return false; // 无效物品
                }
            }
        }

        return bagCount == 1 && dyeCount == 1;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingContainer inv, @NotNull RegistryAccess registryAccess) {
        ItemStack bag = ItemStack.EMPTY;
        DyeItem dye = null;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModItems.ENDER_BAG.get())) {
                    bag = stack.copy();
                } else if (stack.getItem() instanceof DyeItem) {
                    dye = (DyeItem) stack.getItem();
                }
            }
        }

        if (!bag.isEmpty() && dye != null) {
            ItemStack result = bag.copy();
            result.setCount(1);
            CompoundTag tag = result.getOrCreateTag();
            CompoundTag display = tag.getCompound("display");
            int color = dye.getDyeColor().getTextColor();
            // 验证颜色值是否合法（可选）
            for (DyeColor dyeColor : DyeColor.values()) {
                if (color == dyeColor.getTextColor()) {
                    display.putInt("color", color);
                    tag.put("display", display);
                    result.setTag(tag);
                    return result;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.DYE_BAG_RECIPE.get();
    }

    public static class Serializer implements RecipeSerializer<DyeBagRecipe> {
        @Override
        public @NotNull DyeBagRecipe fromJson(@NotNull ResourceLocation recipeId, JsonObject json) {
            String group = json.get("group").getAsString();
            return getDyeBagRecipe(recipeId, group);
        }

        @Override
        public DyeBagRecipe fromNetwork(@NotNull ResourceLocation recipeId, FriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            return getDyeBagRecipe(recipeId, group);
        }

        @NotNull
        private DyeBagRecipe getDyeBagRecipe(@NotNull ResourceLocation recipeId, String group) {
            NonNullList<Ingredient> ingredients = NonNullList.create();
            ingredients.add(Ingredient.of(ModItems.ENDER_BAG.get()));
            ingredients.add(Ingredient.of(net.minecraft.tags.TagKey.create(Registries.ITEM, new ResourceLocation("forge", "dyes"))));
            ItemStack result = new ItemStack(ModItems.ENDER_BAG.get());
            return new DyeBagRecipe(recipeId, group, result, ingredients);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, DyeBagRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
        }
    }
}