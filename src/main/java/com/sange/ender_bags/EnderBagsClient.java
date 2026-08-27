package com.sange.ender_bags;

import com.sange.ender_bags.container.EnderBagScreen;
import com.sange.ender_bags.container.ModMenus;
import com.sange.ender_bags.item.BagItem;
import com.sange.ender_bags.item.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = EnderBags.MOD_ID, value = Dist.CLIENT)
public final class EnderBagsClient {
    private EnderBagsClient() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ENDER_BAG.get(), EnderBagScreen::new);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? BagItem.getTintColor(stack) : -1,
                ModItems.ENDER_BAG.get());
    }
}
