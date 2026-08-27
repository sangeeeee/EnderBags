package com.sange.ender_bags.container;

import com.sange.ender_bags.EnderBags;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, EnderBags.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<EnderBagMenu>> ENDER_BAG =
            MENUS.register("ender_bag", () -> IMenuTypeExtension.create(EnderBagMenu::fromNetwork));

    private ModMenus() {
    }
}
