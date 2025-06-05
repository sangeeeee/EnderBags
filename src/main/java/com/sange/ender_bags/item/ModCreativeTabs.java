package com.sange.ender_bags.item;

import com.sange.ender_bags.EnderBags;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

//public class ModCreativeTabs {
//    public static final DeferredRegister<CreativeModeTab> TABS =
//            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EnderBags.MOD_ID);
//
//    public static final RegistryObject<CreativeModeTab> ENDER_BAGS_TAB = TABS.register("ender_bags_tab",
//            () -> CreativeModeTab.builder()
//                    .title(Component.translatable("itemGroup.ender_bags"))
//                    .icon(() -> new ItemStack(ModItems.ENDER_BAG.get()))
//                    .displayItems((parameters, output) -> {
//                        output.accept(ModItems.ENDER_BAG.get());
//                    })
//                    .build());
//}

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EnderBags.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ENDER_BAGS_TAB = TABS.register("ender_bags_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ender_bags"))
                    .icon(() -> new ItemStack(ModItems.ENDER_BAG.get()))
                    .displayItems((parameters, output) -> {
                        // 添加所有颜色的末影袋
                        for (DyeColor color : DyeColor.values()) {
                            ItemStack stack = new ItemStack(ModItems.ENDER_BAG.get());
                            CompoundTag tag = new CompoundTag();
                            CompoundTag display = new CompoundTag();
                            display.putInt("color", color.getTextColor());
                            tag.put("display", display);
                            stack.setTag(tag);
                            output.accept(stack);
                        }
                    })
                    .build());
}