package com.sange.ender_bags.item;

import com.sange.ender_bags.container.EnderBagMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;



public class BagItem extends Item implements MenuProvider {
    public BagItem() {
        super(new Item.Properties()
                .stacksTo(1) // Storage bag typically has max stack size of 1
        );
    }

    @Override
    public @NotNull Component getName(ItemStack stack) {
        CompoundTag display = stack.getTagElement("display");
        int colorValue = display != null && display.contains("color", 99) ? display.getInt("color") : DyeColor.WHITE.getTextColor();
        String colorName = "white"; // 默认白色
        for (DyeColor dyeColor : DyeColor.values()) {
            if (dyeColor.getTextColor() == colorValue) {
                colorName = dyeColor.getName();
                break;
            }
        }
        return Component.translatable("item.ender_bags.ender_bag." + colorName);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !level.isClientSide && !(player.containerMenu instanceof EnderBagMenu)) {
            NetworkHooks.openScreen((ServerPlayer) player, this, buf -> buf.writeEnum(hand));
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    public static ItemStackHandler getHandlerForContainer(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ItemStackHandler handler = new ItemStackHandler(104); // 8x13 slots
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("inv")) {
                handler.deserializeNBT(tag.getCompound("inv"));
            }
        }
        return handler;
    }



    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.ender_bags.ender_bag");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, Player player) {
        ItemStack bag = player.getItemInHand(player.getUsedItemHand());
        return new EnderBagMenu(id, playerInventory, bag, player.getUsedItemHand());
    }

}