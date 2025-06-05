package com.sange.ender_bags.recipe;

import com.sange.ender_bags.EnderBags;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, EnderBags.MOD_ID);

    public static final RegistryObject<RecipeSerializer<?>> DYE_BAG_RECIPE =
            RECIPE_SERIALIZERS.register("dye_ender_bag", DyeBagRecipe.Serializer::new);
}