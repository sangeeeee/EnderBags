package com.sange.ender_bags.item;

import com.sange.ender_bags.container.EnderBagMenu;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

public final class BagItem extends Item {
    private static final int TOOLTIP_ENTRY_LIMIT = 4;

    public BagItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        DyeColor color = getDyeColor(stack);
        return Component.translatable("item.ender_bags.ender_bag." + color.getName());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND || !stack.is(this)) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer) || player.containerMenu instanceof EnderBagMenu) {
            return InteractionResultHolder.fail(stack);
        }

        int selectedSlot = player.getInventory().selected;
        ItemStack selectedStack = player.getInventory().getItem(selectedSlot);
        if (selectedStack != stack) {
            return InteractionResultHolder.fail(stack);
        }

        BagContents.migrateLegacyData(stack, level.registryAccess());
        player.getInventory().setChanged();

        SimpleMenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) ->
                        new EnderBagMenu(containerId, inventory, selectedSlot, selectedStack),
                Component.translatable("container.ender_bags.ender_bag"));

        if (serverPlayer.openMenu(provider, buffer -> buffer.writeVarInt(selectedSlot)).isPresent()) {
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.BUNDLE_INSERT,
                    SoundSource.PLAYERS,
                    1.0F,
                    level.random.nextFloat() * 0.1F + 0.9F);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        HolderLookup.Provider registries = context.registries();
        Iterable<ItemStack> storedStacks;
        if (registries != null) {
            storedStacks = BagContents.load(stack, registries);
        } else {
            storedStacks = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                    .nonEmptyItemsCopy();
        }

        int occupiedSlots = 0;
        for (ItemStack storedStack : storedStacks) {
            if (storedStack.isEmpty()) {
                continue;
            }
            if (occupiedSlots < TOOLTIP_ENTRY_LIMIT) {
                MutableComponent line = storedStack.getHoverName().copy();
                line.append(Component.literal(" x" + storedStack.getCount()));
                tooltip.add(line);
            }
            occupiedSlots++;
        }

        int hiddenSlots = occupiedSlots - TOOLTIP_ENTRY_LIMIT;
        if (hiddenSlots > 0) {
            tooltip.add(Component.translatable("item.ender_bags.ender_bag.more", hiddenSlots)
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    @Override
    public int getEntityLifespan(ItemStack itemStack, Level level) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        entity.setUnlimitedLifetime();
        return false;
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return false;
    }

    @Deprecated
    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    public static ItemStack createColoredStack(DyeColor color) {
        ItemStack stack = ModItems.ENDER_BAG.toStack();
        if (color != DyeColor.WHITE) {
            stack.set(DataComponents.DYED_COLOR,
                    new DyedItemColor(color.getTextureDiffuseColor() & 0xFFFFFF, false));
        }
        return stack;
    }

    public static int getTintColor(ItemStack stack) {
        DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
        if (dyedColor == null) {
            return -1;
        }
        return 0xFF000000 | (dyedColor.rgb() & 0xFFFFFF);
    }

    public static DyeColor getDyeColor(ItemStack stack) {
        DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
        if (dyedColor == null) {
            return DyeColor.WHITE;
        }

        int rgb = dyedColor.rgb() & 0xFFFFFF;
        for (DyeColor color : DyeColor.values()) {
            if ((color.getTextureDiffuseColor() & 0xFFFFFF) == rgb
                    || (color.getTextColor() & 0xFFFFFF) == rgb) {
                return color;
            }
        }
        return DyeColor.WHITE;
    }
}
