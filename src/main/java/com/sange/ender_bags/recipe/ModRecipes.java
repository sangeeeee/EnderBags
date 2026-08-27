package com.sange.ender_bags.recipe;

import com.sange.ender_bags.EnderBags;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, EnderBags.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DyeBagRecipe>> DYE_BAG_RECIPE =
            RECIPE_SERIALIZERS.register(
                    "dye_ender_bag",
                    () -> new SimpleCraftingRecipeSerializer<>(DyeBagRecipe::new));

    private ModRecipes() {
    }
}
