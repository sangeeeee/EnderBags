
package com.sange.ender_bags;

import com.sange.ender_bags.item.ModItems;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@SuppressWarnings("deprecation")
@Mod.EventBusSubscriber(modid = EnderBags.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        ItemColors itemColors = event.getItemColors();
        itemColors.register((stack, tintIndex) -> {
            if (tintIndex == 0) {
                CompoundTag display = stack.getTagElement("display");
                if (display != null && display.contains("color", 99)) {
                    int color = display.getInt("color");
                    if (color == DyeColor.WHITE.getTextColor()) {
                        return -1; // Use original texture
                    }
                    // Find matching DyeColor
                    for (DyeColor dyeColor : DyeColor.values()) {
                        if (dyeColor.getTextColor() == color) {
                            // Adjust color using getTextureDiffuseColors
                            float[] diffuseColors = dyeColor.getTextureDiffuseColors();
                            int r = (int) (255 * diffuseColors[0]);
                            int g = (int) (255 * diffuseColors[1]);
                            int b = (int) (255 * diffuseColors[2]);
                            return (r << 16) | (g << 8) | b; // RGB, no alpha
                        }
                    }
                    return color; // Fallback to original color
                }
                return -1; // Use original texture
            }
            return -1;
        }, ModItems.ENDER_BAG.get());
    }
}