package com.sange.ender_bags.item;

import com.mojang.serialization.Dynamic;
import com.sange.ender_bags.EnderBags;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemContainerContents;

/** Immutable snapshots and one-time data migration for Ender Bag item stacks. */
public final class BagContents {
    public static final int SLOT_COUNT = 8 * 13;

    // Minecraft 1.20.1 data version, used by the attached legacy implementation.
    private static final int LEGACY_DATA_VERSION = 3465;
    private static final String LEGACY_INVENTORY_KEY = "inv";
    private static final int MAX_LEGACY_ENTRIES = SLOT_COUNT * 4;

    private BagContents() {
    }

    /**
     * Checks whether a bag currently contains any item without mutating it or requiring
     * registry access. Unknown legacy inventory data is treated as non-empty so that a
     * malformed old bag does not accidentally lose its item-entity protection.
     */
    public static boolean hasStoredItems(ItemStack bagStack) {
        ItemContainerContents stored = bagStack.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY);
        if (stored.nonEmptyItems().iterator().hasNext()) {
            return true;
        }

        CustomData customData = bagStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return false;
        }

        CompoundTag root = customData.copyTag();
        if (!root.contains(LEGACY_INVENTORY_KEY)) {
            return false;
        }
        if (!root.contains(LEGACY_INVENTORY_KEY, Tag.TAG_COMPOUND)) {
            return true;
        }

        Tag legacyItems = root.getCompound(LEGACY_INVENTORY_KEY).get("Items");
        if (legacyItems == null) {
            return false;
        }
        return !(legacyItems instanceof ListTag list) || !list.isEmpty();
    }

    /**
     * Reads a detached snapshot. Invalid or conflicting data returns an empty Optional and
     * is deliberately left untouched, preventing silent item deletion during recovery.
     */
    public static Optional<NonNullList<ItemStack>> load(
            ItemStack bagStack,
            HolderLookup.Provider registries) {
        if (!bagStack.is(ModItems.ENDER_BAG.get())) {
            return Optional.empty();
        }

        ItemContainerContents currentContents = bagStack.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY);
        Optional<NonNullList<ItemStack>> current = readCurrentContents(currentContents);
        if (current.isEmpty()) {
            return Optional.empty();
        }

        CustomData customData = bagStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(LEGACY_INVENTORY_KEY)) {
            return current;
        }

        CompoundTag root = customData.copyTag();
        if (!root.contains(LEGACY_INVENTORY_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }

        Optional<NonNullList<ItemStack>> legacy = readLegacyInventory(
                root.getCompound(LEGACY_INVENTORY_KEY),
                registries);
        if (legacy.isEmpty()) {
            return Optional.empty();
        }

        if (bagStack.has(DataComponents.CONTAINER)) {
            ItemContainerContents legacyContents = ItemContainerContents.fromItems(legacy.get());
            return legacyContents.equals(currentContents) ? current : Optional.empty();
        }
        return legacy;
    }

    /**
     * Commits one complete immutable contents snapshot to the bag. The component patch is
     * validated and rolled back by ItemStack if it would create an invalid stack.
     */
    public static boolean save(ItemStack bagStack, List<ItemStack> stacks) {
        CustomData customData = bagStack.get(DataComponents.CUSTOM_DATA);
        ItemContainerContents existingContents = bagStack.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY);
        if (!bagStack.is(ModItems.ENDER_BAG.get())
                || stacks.size() != SLOT_COUNT
                || !containsOnlyValidStacks(stacks)
                || (customData != null && customData.contains(LEGACY_INVENTORY_KEY))
                || readCurrentContents(existingContents).isEmpty()) {
            return false;
        }

        final ItemContainerContents contents;
        try {
            contents = ItemContainerContents.fromItems(stacks);
        } catch (RuntimeException exception) {
            EnderBags.LOGGER.error("Could not snapshot Ender Bag contents", exception);
            return false;
        }

        DataComponentPatch.Builder patch = DataComponentPatch.builder();
        if (contents.equals(ItemContainerContents.EMPTY)) {
            patch.remove(DataComponents.CONTAINER);
        } else {
            patch.set(DataComponents.CONTAINER, contents);
        }

        bagStack.applyComponentsAndValidate(patch.build());
        boolean committed = bagStack.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY).equals(contents);
        if (!committed) {
            EnderBags.LOGGER.error("Rejected an invalid Ender Bag contents snapshot");
        }
        return committed;
    }

    /**
     * Migrates old NBT with a single validated component patch. Migration is idempotent. If
     * legacy and current contents conflict, or any legacy entry cannot be decoded losslessly,
     * the original data is preserved and this method returns false.
     */
    public static boolean migrateLegacyData(
            ItemStack bagStack,
            HolderLookup.Provider registries) {
        Optional<NonNullList<ItemStack>> readable = load(bagStack, registries);
        if (readable.isEmpty()) {
            return false;
        }

        CustomData customData = bagStack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = customData == null ? new CompoundTag() : customData.copyTag();
        boolean hadLegacyInventory = root.contains(LEGACY_INVENTORY_KEY);
        boolean customDataChanged = false;

        if (hadLegacyInventory) {
            root.remove(LEGACY_INVENTORY_KEY);
            customDataChanged = true;
        }

        DyedItemColor oldColor = bagStack.get(DataComponents.DYED_COLOR);
        DyedItemColor newColor = normalizeLegacyDyedColor(oldColor);

        if (root.contains("display", Tag.TAG_COMPOUND)) {
            CompoundTag display = root.getCompound("display");
            if (display.contains("color", Tag.TAG_ANY_NUMERIC)) {
                DyeColor legacyColor = findLegacyTextColor(display.getInt("color"));
                if (legacyColor != null) {
                    if (newColor == null && legacyColor != DyeColor.WHITE) {
                        newColor = new DyedItemColor(
                                legacyColor.getTextureDiffuseColor() & 0xFFFFFF,
                                false);
                    }
                    display.remove("color");
                    if (display.isEmpty()) {
                        root.remove("display");
                    } else {
                        root.put("display", display);
                    }
                    customDataChanged = true;
                }
            }
        }

        boolean colorChanged = !Objects.equals(oldColor, newColor);
        if (!hadLegacyInventory && !customDataChanged && !colorChanged) {
            return true;
        }

        DataComponentPatch.Builder patch = DataComponentPatch.builder();
        ItemContainerContents migratedContents = null;
        if (hadLegacyInventory && !bagStack.has(DataComponents.CONTAINER)) {
            migratedContents = ItemContainerContents.fromItems(readable.get());
            if (migratedContents.equals(ItemContainerContents.EMPTY)) {
                patch.remove(DataComponents.CONTAINER);
            } else {
                patch.set(DataComponents.CONTAINER, migratedContents);
            }
        }

        if (customDataChanged) {
            if (root.isEmpty()) {
                patch.remove(DataComponents.CUSTOM_DATA);
            } else {
                patch.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
            }
        }

        if (colorChanged) {
            if (newColor == null) {
                patch.remove(DataComponents.DYED_COLOR);
            } else {
                patch.set(DataComponents.DYED_COLOR, newColor);
            }
        }

        try {
            bagStack.applyComponentsAndValidate(patch.build());
        } catch (RuntimeException exception) {
            EnderBags.LOGGER.error("Could not atomically migrate an Ender Bag", exception);
            return false;
        }

        if (migratedContents != null
                && !bagStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                        .equals(migratedContents)) {
            return false;
        }
        if (customDataChanged) {
            CustomData actual = bagStack.get(DataComponents.CUSTOM_DATA);
            if (root.isEmpty() ? actual != null : !CustomData.of(root).equals(actual)) {
                return false;
            }
        }
        return !colorChanged || Objects.equals(newColor, bagStack.get(DataComponents.DYED_COLOR));
    }

    private static Optional<NonNullList<ItemStack>> readCurrentContents(
            ItemContainerContents stored) {
        try {
            for (int slot = SLOT_COUNT; slot < stored.getSlots(); slot++) {
                if (!stored.getStackInSlot(slot).isEmpty()) {
                    return Optional.empty();
                }
            }

            NonNullList<ItemStack> stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
            stored.copyInto(stacks);
            return containsOnlyValidStacks(stacks) ? Optional.of(stacks) : Optional.empty();
        } catch (RuntimeException exception) {
            EnderBags.LOGGER.warn("Could not read current Ender Bag contents", exception);
            return Optional.empty();
        }
    }

    private static Optional<NonNullList<ItemStack>> readLegacyInventory(
            CompoundTag inventoryTag,
            HolderLookup.Provider registries) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        Tag rawItems = inventoryTag.get("Items");
        if (rawItems == null) {
            return Optional.of(stacks);
        }
        if (!(rawItems instanceof ListTag items)
                || (!items.isEmpty() && items.getElementType() != Tag.TAG_COMPOUND)
                || items.size() > MAX_LEGACY_ENTRIES) {
            return Optional.empty();
        }

        boolean[] occupiedSlots = new boolean[SLOT_COUNT];
        for (int index = 0; index < items.size(); index++) {
            CompoundTag entry = items.getCompound(index);
            if (!entry.contains("Slot", Tag.TAG_ANY_NUMERIC)) {
                return Optional.empty();
            }

            int slot = entry.getInt("Slot");
            if (slot < 0 || slot >= SLOT_COUNT || occupiedSlots[slot]) {
                return Optional.empty();
            }

            CompoundTag itemData = entry.copy();
            itemData.remove("Slot");
            Optional<ItemStack> parsed = parseLegacyItem(itemData, registries);
            if (parsed.isEmpty()
                    || parsed.get().isEmpty()
                    || parsed.get().getCount() > parsed.get().getMaxStackSize()) {
                return Optional.empty();
            }

            occupiedSlots[slot] = true;
            stacks.set(slot, parsed.get());
        }
        return Optional.of(stacks);
    }

    private static Optional<ItemStack> parseLegacyItem(
            CompoundTag itemData,
            HolderLookup.Provider registries) {
        boolean legacyFormat = itemData.contains("Count", Tag.TAG_ANY_NUMERIC)
                || itemData.contains("tag", Tag.TAG_COMPOUND);
        if (!legacyFormat) {
            Optional<ItemStack> currentFormat = ItemStack.parse(registries, itemData);
            if (currentFormat.isPresent()) {
                return currentFormat;
            }
        }

        try {
            Tag fixed = DataFixers.getDataFixer()
                    .update(
                            References.ITEM_STACK,
                            new Dynamic<>(NbtOps.INSTANCE, itemData),
                            LEGACY_DATA_VERSION,
                            SharedConstants.getCurrentVersion().getDataVersion().getVersion())
                    .getValue();
            if (fixed instanceof CompoundTag fixedItem) {
                return ItemStack.parse(registries, fixedItem);
            }
        } catch (RuntimeException exception) {
            EnderBags.LOGGER.debug("Could not migrate an item stored in a legacy Ender Bag", exception);
        }
        return Optional.empty();
    }

    private static boolean containsOnlyValidStacks(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack == null
                    || (!stack.isEmpty()
                            && (stack.getCount() <= 0 || stack.getCount() > stack.getMaxStackSize()))) {
                return false;
            }
        }
        return true;
    }

    private static DyedItemColor normalizeLegacyDyedColor(DyedItemColor color) {
        if (color == null) {
            return null;
        }

        DyeColor legacyColor = findLegacyTextColor(color.rgb());
        if (legacyColor == null) {
            return color;
        }
        if (legacyColor == DyeColor.WHITE) {
            return null;
        }
        return new DyedItemColor(
                legacyColor.getTextureDiffuseColor() & 0xFFFFFF,
                color.showInTooltip());
    }

    private static DyeColor findLegacyTextColor(int rgb) {
        int normalized = rgb & 0xFFFFFF;
        for (DyeColor color : DyeColor.values()) {
            if ((color.getTextColor() & 0xFFFFFF) == normalized) {
                return color;
            }
        }
        return null;
    }
}
