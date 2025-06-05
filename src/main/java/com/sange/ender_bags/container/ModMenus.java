
package com.sange.ender_bags.container;

import com.sange.ender_bags.EnderBags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, EnderBags.MOD_ID);

    public static final RegistryObject<MenuType<EnderBagMenu>> ENDER_BAG =
            MENUS.register("ender_bag", () -> IForgeMenuType.create((id, inventory, data) -> {
                InteractionHand hand = data.readEnum(InteractionHand.class);
                ItemStack bag = inventory.player.getItemInHand(hand);
                return new EnderBagMenu(id, inventory, bag, hand);
            }));
}