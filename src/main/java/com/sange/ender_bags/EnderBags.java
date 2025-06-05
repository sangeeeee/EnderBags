package com.sange.ender_bags;

import com.sange.ender_bags.container.EnderBagScreen;
import com.sange.ender_bags.container.ModMenus;
import com.sange.ender_bags.item.ModCreativeTabs;
import com.sange.ender_bags.item.ModItems;
import com.sange.ender_bags.recipe.ModRecipes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@SuppressWarnings("removal")
@Mod(com.sange.ender_bags.EnderBags.MOD_ID)
public class EnderBags {
    public static final String MOD_ID = "ender_bags";

    public EnderBags() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register items
        ModItems.ITEMS.register(modEventBus);

        ModCreativeTabs.TABS.register(modEventBus);

        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);

        ModMenus.MENUS.register(modEventBus);

        modEventBus.addListener(this::clientSetup);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.ENDER_BAG.get(), EnderBagScreen::new);
        });
    }

}