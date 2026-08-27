package com.sange.ender_bags.item;

import com.mojang.serialization.Dynamic;
import com.sange.ender_bags.EnderBags;
import java.util.List;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
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

/** Storage and one-time data migration for Ender Bag item stacks. */
public final class BagContents {
    public static final int SLOT_COUNT = 8 * 13;

    // Minecraft 1.20.1 data version, used by the attached legacy implementation.
    private static final int LEGACY_DATA_VERSION = 3465;
    private static final String LEGACY_INVENTORY_KEY = "inv";
    private static final int MAX_LEGACY_ENTRIES = SLOT_COUNT * 4;

    private BagContents() {
    }

    public static NonNullList<ItemStack> load(ItemStack bagStack, HolderLookup.Provider registries) {
        migrateLegacyData(bagStack, registries);

        NonNullList<ItemStack> stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ItemContainerContents stored = bagStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        stored.copyInto(stacks);

        boolean sanitized = stored.getSlots() > SLOT_COUNT;
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack storedStack = stacks.get(slot);
            if (storedStack.getItem() instanceof BagItem) {
                stacks.set(slot, ItemStack.EMPTY);
                sanitized = true;
            } else if (!storedStack.isEmpty() && storedStack.getCount() > storedStack.getMaxStackSize()) {
                storedStack.setCount(storedStack.getMaxStackSize());
                sanitized = true;
            }
        }

        if (sanitized) {
            save(bagStack, stacks);
        }
        return stacks;
    }

    public static void save(ItemStack bagStack, List<ItemStack> stacks) {
        ItemContainerContents contents = ItemContainerContents.fromItems(stacks);
        if (contents.equals(ItemContainerContents.EMPTY)) {
            bagStack.remove(DataComponents.CONTAINER);
        } else {
            bagStack.set(DataComponents.CONTAINER, contents);
        }
    }

    public static void migrateLegacyData(ItemStack bagStack, HolderLookup.Provider registries) {
        normalizeLegacyDyedColor(bagStack);

        CustomData customData = bagStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return;
        }

        CompoundTag root = customData.copyTag();
        boolean changed = false;

        if (root.contains(LEGACY_INVENTORY_KEY)) {
            if (!bagStack.has(DataComponents.CONTAINER)
                    && root.contains(LEGACY_INVENTORY_KEY, Tag.TAG_COMPOUND)) {
                NonNullList<ItemStack> migrated =
                        readLegacyInventory(root.getCompound(LEGACY_INVENTORY_KEY), registries);
                save(bagStack, migrated);
            }
            root.remove(LEGACY_INVENTORY_KEY);
            changed = true;
        }

        if (root.contains("display", Tag.TAG_COMPOUND)) {
            CompoundTag display = root.getCompound("display");
            if (display.contains("color", Tag.TAG_ANY_NUMERIC)) {
                if (!bagStack.has(DataComponents.DYED_COLOR)) {
                    DyeColor color = findLegacyTextColor(display.getInt("color"));
                    if (color != null && color != DyeColor.WHITE) {
                        bagStack.set(DataComponents.DYED_COLOR,
                                new DyedItemColor(color.getTextureDiffuseColor() & 0xFFFFFF, false));
                    }
                }
                display.remove("color");
                if (display.isEmpty()) {
                    root.remove("display");
                } else {
                    root.put("display", display);
                }
                changed = true;
            }
        }

        if (changed) {
            CustomData.set(DataComponents.CUSTOM_DATA, bagStack, root);
        }
    }

    private static NonNullList<ItemStack> readLegacyInventory(
            CompoundTag inventoryTag,
            HolderLookup.Provider registries) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ListTag items = inventoryTag.getList("Items", Tag.TAG_COMPOUND);
        int entriesToRead = Math.min(items.size(), MAX_LEGACY_ENTRIES);

        for (int index = 0; index < entriesToRead; index++) {
            CompoundTag entry = items.getCompound(index);
            if (!entry.contains("Slot", Tag.TAG_ANY_NUMERIC)) {
                continue;
            }
            int slot = entry.getInt("Slot");
            if (slot < 0 || slot >= SLOT_COUNT) {
                continue;
            }

            CompoundTag itemData = entry.copy();
            itemData.remove("Slot");
            parseLegacyItem(itemData, registries).ifPresent(stack -> {
                if (!(stack.getItem() instanceof BagItem)) {
                    stack.setCount(Math.min(stack.getCount(), stack.getMaxStackSize()));
                    stacks.set(slot, stack);
                }
            });
        }
        return stacks;
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

    private static void normalizeLegacyDyedColor(ItemStack stack) {
        DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
        if (dyedColor == null) {
            return;
        }

        DyeColor legacyColor = findLegacyTextColor(dyedColor.rgb());
        if (legacyColor == null) {
            return;
        }

        if (legacyColor == DyeColor.WHITE) {
            stack.remove(DataComponents.DYED_COLOR);
        } else {
            stack.set(DataComponents.DYED_COLOR,
                    new DyedItemColor(legacyColor.getTextureDiffuseColor() & 0xFFFFFF, dyedColor.showInTooltip()));
        }
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
