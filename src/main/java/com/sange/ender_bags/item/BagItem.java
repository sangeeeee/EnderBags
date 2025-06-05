package com.sange.ender_bags.item;

import com.sange.ender_bags.container.EnderBagMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.List;


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
            // Play open sound
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS,
                    1.0F, level.random.nextFloat() * 0.1F + 0.9F);
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("inv")) {
            CompoundTag invTag = tag.getCompound("inv");
            ListTag items = invTag.getList("Items", 10); // CompoundTag
            int displayed = 0;
            int totalItems = 0;
            for (int i = 0; i < items.size() && displayed <= 4; i++) {
                CompoundTag itemTag = items.getCompound(i);
                ItemStack item = ItemStack.of(itemTag);
                if (!item.isEmpty()) {
                    totalItems++;
                    if (displayed < 4) {
                        MutableComponent itemTooltip = item.getHoverName().copy();
                        itemTooltip.append(" x").append(String.valueOf(item.getCount()));
                        tooltip.add(itemTooltip);
                        displayed++;
                    }
                }
            }
            if (totalItems - displayed > 0) {
                tooltip.add(Component.translatable("item.ender_bags.ender_bag.more", totalItems - displayed)
                        .withStyle(ChatFormatting.ITALIC));
            }
        }
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