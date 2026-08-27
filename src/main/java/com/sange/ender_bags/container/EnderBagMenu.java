package com.sange.ender_bags.container;

import com.sange.ender_bags.EnderBags;
import com.sange.ender_bags.item.BagContents;
import com.sange.ender_bags.item.BagItem;
import com.sange.ender_bags.item.ModItems;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public final class EnderBagMenu extends AbstractContainerMenu {
    public static final int BAG_SLOT_COUNT = BagContents.SLOT_COUNT;

    private static final int HOTBAR_SIZE = 9;
    private static final int PLAYER_INVENTORY_ROWS = 3;
    private static final int PLAYER_INVENTORY_START = BAG_SLOT_COUNT;
    private static final int HOTBAR_START = PLAYER_INVENTORY_START + PLAYER_INVENTORY_ROWS * HOTBAR_SIZE;
    private static final int PLAYER_SLOTS_END = HOTBAR_START + HOTBAR_SIZE;

    private final Inventory playerInventory;
    private final int bagInventorySlot;
    private final int lockedMenuSlot;
    private final ItemStack bagStack;
    private final BagInventory bagInventory;
    private boolean storageWritable;

    public EnderBagMenu(int containerId, Inventory playerInventory, int bagInventorySlot, ItemStack bagStack) {
        super(ModMenus.ENDER_BAG.get(), containerId);
        if (bagInventorySlot < 0 || bagInventorySlot >= HOTBAR_SIZE) {
            throw new IllegalArgumentException("Ender Bag must be opened from a hotbar slot");
        }

        this.playerInventory = playerInventory;
        this.bagInventorySlot = bagInventorySlot;
        this.lockedMenuSlot = HOTBAR_START + bagInventorySlot;
        this.bagStack = bagStack;
        NonNullList<ItemStack> storedItems;
        if (playerInventory.player.level().isClientSide) {
            storedItems = NonNullList.withSize(BAG_SLOT_COUNT, ItemStack.EMPTY);
            this.storageWritable = true;
        } else {
            boolean migrated = BagContents.migrateLegacyData(
                    bagStack,
                    playerInventory.player.level().registryAccess());
            Optional<NonNullList<ItemStack>> loaded = migrated
                    ? BagContents.load(bagStack, playerInventory.player.level().registryAccess())
                    : Optional.empty();
            storedItems = loaded.orElseGet(
                    () -> NonNullList.withSize(BAG_SLOT_COUNT, ItemStack.EMPTY));
            this.storageWritable = loaded.isPresent();
        }
        this.bagInventory = new BagInventory(storedItems);

        addBagSlots();
        addPlayerSlots();
    }

    public static EnderBagMenu fromNetwork(
            int containerId,
            Inventory playerInventory,
            RegistryFriendlyByteBuf buffer) {
        int bagInventorySlot = buffer.readVarInt();
        if (bagInventorySlot < 0 || bagInventorySlot >= HOTBAR_SIZE) {
            throw new IllegalArgumentException("Received invalid Ender Bag hotbar slot: " + bagInventorySlot);
        }
        return new EnderBagMenu(
                containerId,
                playerInventory,
                bagInventorySlot,
                playerInventory.getItem(bagInventorySlot));
    }

    private void addBagSlots() {
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 13; column++) {
                addSlot(new BagSlot(
                        bagInventory,
                        column + row * 13,
                        12 + column * 18,
                        5 + row * 18));
            }
        }
    }

    private void addPlayerSlots() {
        for (int row = 0; row < PLAYER_INVENTORY_ROWS; row++) {
            for (int column = 0; column < HOTBAR_SIZE; column++) {
                addSlot(new Slot(
                        playerInventory,
                        column + row * HOTBAR_SIZE + HOTBAR_SIZE,
                        48 + column * 18,
                        152 + row * 18));
            }
        }

        for (int column = 0; column < HOTBAR_SIZE; column++) {
            Slot slot = column == bagInventorySlot
                    ? new LockedBagSlot(playerInventory, column, 48 + column * 18, 210)
                    : new Slot(playerInventory, column, 48 + column * 18, 210);
            addSlot(slot);
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (!storageWritable
                || player != playerInventory.player
                || player.getInventory().selected != bagInventorySlot) {
            return false;
        }

        ItemStack currentStack = playerInventory.getItem(bagInventorySlot);
        if (!currentStack.is(ModItems.ENDER_BAG.get())) {
            return false;
        }

        // Server-side identity binding prevents switching to another bag while this menu is open.
        return player.level().isClientSide || currentStack == bagStack;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!stillValid(player)
                || slotId == lockedMenuSlot
                || (clickType == ClickType.SWAP && button == bagInventorySlot)) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        if (!stillValid(player)
                || slotIndex < 0
                || slotIndex >= slots.size()
                || slotIndex == lockedMenuSlot) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = slot.getItem();
        ItemStack originalStack = sourceStack.copy();

        if (slotIndex < BAG_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_SLOTS_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(sourceStack, 0, BAG_SLOT_COUNT, false)) {
            if (slotIndex < HOTBAR_START) {
                if (!moveItemStackTo(sourceStack, HOTBAR_START, PLAYER_SLOTS_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (sourceStack.getCount() == originalStack.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, sourceStack);
        return originalStack;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        saveBagInventory();
        if (!player.level().isClientSide) {
            player.level().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.BUNDLE_DROP_CONTENTS,
                    SoundSource.PLAYERS,
                    1.0F,
                    player.level().random.nextFloat() * 0.1F + 0.9F);
        }
    }

    private void saveBagInventory() {
        if (!playerInventory.player.level().isClientSide
                && storageWritable
                && bagStack.is(ModItems.ENDER_BAG.get())) {
            if (BagContents.save(bagStack, bagInventory.stacksView())) {
                playerInventory.setChanged();
            } else {
                storageWritable = false;
                EnderBags.LOGGER.error(
                        "Closed an Ender Bag menu after its contents snapshot was rejected");
            }
        }
    }

    private final class BagInventory extends ItemStackHandler {
        private BagInventory(NonNullList<ItemStack> stacks) {
            super(stacks);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return !(stack.getItem() instanceof BagItem);
        }

        @Override
        protected void onContentsChanged(int slot) {
            saveBagInventory();
        }

        private void markContentsChanged(int slot) {
            onContentsChanged(slot);
        }

        private NonNullList<ItemStack> stacksView() {
            return stacks;
        }
    }

    private final class BagSlot extends SlotItemHandler {
        private BagSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public void setChanged() {
            bagInventory.markContentsChanged(index);
        }
    }

    private static final class LockedBagSlot extends Slot {
        private LockedBagSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }
    }
}
