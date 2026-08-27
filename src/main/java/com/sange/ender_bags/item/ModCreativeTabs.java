package com.sange.ender_bags.item;

import com.sange.ender_bags.EnderBags;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EnderBags.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENDER_BAGS_TAB =
            TABS.register("ender_bags_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ender_bags"))
                    .icon(() -> BagItem.createColoredStack(DyeColor.WHITE))
                    .displayItems((parameters, output) -> {
                        for (DyeColor color : DyeColor.values()) {
                            output.accept(BagItem.createColoredStack(color));
                        }
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
