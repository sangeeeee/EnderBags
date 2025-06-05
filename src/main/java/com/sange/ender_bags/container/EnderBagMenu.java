
package com.sange.ender_bags.container;

import com.sange.ender_bags.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class EnderBagMenu extends AbstractContainerMenu {
    public static final int BAG_SLOTS = 8 * 13; // 104 slots
    public static final int INVENTORY_SLOTS = BAG_SLOTS; // Start of player inventory
    public static final int HOTBAR_SLOTS = INVENTORY_SLOTS + 3 * 9; // Start of hotbar

    private final ItemStackHandler bagInventory;
    private final InteractionHand bagHand;
    private final ItemStack bagStack;
    private final Inventory playerInventory;

    public EnderBagMenu(int id, Inventory playerInventory, ItemStack bagStack, InteractionHand hand) {
        super(ModMenus.ENDER_BAG.get(), id);
        if (hand != InteractionHand.MAIN_HAND) {
            throw new IllegalStateException("EnderBagMenu can only be opened with MAIN_HAND");
        }
        this.bagStack = bagStack;
        this.bagHand = hand;
        this.playerInventory = playerInventory;
        this.bagInventory = new ItemStackHandler(104) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return !stack.is(ModItems.ENDER_BAG.get()); // Prevent ender_bag placement
            }
        };
        if (bagStack.getTag() != null && bagStack.hasTag() && bagStack.getTag().contains("inv")) {
            this.bagInventory.deserializeNBT(bagStack.getTag().getCompound("inv"));
        }

        // Bag slots (8x13)
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 13; col++) {
                this.addSlot(new SlotItemHandler(bagInventory, col + row * 13, 12 + col * 18, 5 + row * 18));
            }
        }

        // Player inventory (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 48 + col * 18, 152 + row * 18));
            }
        }

        // Hotbar (1x9)
        for (int col = 0; col < 9; col++) {
            if (playerInventory.selected == col) {
                this.addSlot(new LockedBagSlot(playerInventory, col, 48 + col * 18, 210));
            } else {
                this.addSlot(new Slot(playerInventory, col, 48 + col * 18, 210));
            }
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return bagHand == InteractionHand.MAIN_HAND && !bagStack.isEmpty() &&
                player.getItemInHand(bagHand).is(ModItems.ENDER_BAG.get());
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        saveBagInventory();
    }

    @Override
    public void slotsChanged(net.minecraft.world.@NotNull Container container) {
        super.slotsChanged(container);
        // No save here; defer to removed
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        ItemStack slotStack = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (slot.hasItem() && !(slot instanceof LockedBagSlot)) {
            ItemStack stack = slot.getItem();
            slotStack = stack.copy();

            // From bag to player inventory/hotbar
            if (slotIndex < BAG_SLOTS) {
                if (!moveItemStackTo(stack, INVENTORY_SLOTS, HOTBAR_SLOTS + 9, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From player inventory to bag
            else if (slotIndex < HOTBAR_SLOTS) {
                if (!moveItemStackTo(stack, 0, BAG_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // From hotbar to bag or inventory
            else {
                if (!moveItemStackTo(stack, 0, BAG_SLOTS, false) &&
                        !moveItemStackTo(stack, INVENTORY_SLOTS, HOTBAR_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == slotStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return slotStack;
    }

    private void saveBagInventory() {
        if (!playerInventory.player.level().isClientSide && !bagStack.isEmpty()) {
            CompoundTag tag = bagStack.getOrCreateTag();
            tag.put("inv", bagInventory.serializeNBT());
            bagStack.setTag(tag);
            playerInventory.setChanged(); // Mark inventory dirty to trigger player save
        }
    }

    private static class LockedBagSlot extends Slot {
        public LockedBagSlot(net.minecraft.world.Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return false; // Prevent picking up the held bag
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false; // Prevent placing items in the held bag slot
        }
    }
}