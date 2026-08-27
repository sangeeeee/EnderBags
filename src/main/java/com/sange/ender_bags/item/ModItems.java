package com.sange.ender_bags.item;

import com.sange.ender_bags.EnderBags;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnderBags.MOD_ID);

    public static final DeferredItem<BagItem> ENDER_BAG = ITEMS.registerItem(
            "ender_bag",
            BagItem::new,
            new Item.Properties().stacksTo(1).fireResistant());

    private ModItems() {
    }
}
