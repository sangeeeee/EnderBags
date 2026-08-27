package com.sange.ender_bags;

import com.mojang.logging.LogUtils;
import com.sange.ender_bags.container.ModMenus;
import com.sange.ender_bags.item.ModCreativeTabs;
import com.sange.ender_bags.item.ModItems;
import com.sange.ender_bags.recipe.ModRecipes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EnderBags.MOD_ID)
public final class EnderBags {
    public static final String MOD_ID = "ender_bags";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EnderBags(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
    }
}
