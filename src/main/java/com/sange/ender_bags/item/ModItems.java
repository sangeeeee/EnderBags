
package com.sange.ender_bags.item;

;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, com.sange.ender_bags.EnderBags.MOD_ID);

    public static final RegistryObject<Item> ENDER_BAG = ITEMS.register("ender_bag",
            BagItem::new);
}