package com.sange.ender_bags.gametest;

import com.sange.ender_bags.EnderBags;
import com.sange.ender_bags.container.EnderBagMenu;
import com.sange.ender_bags.item.BagContents;
import com.sange.ender_bags.item.BagItem;
import com.sange.ender_bags.item.ModItems;
import com.sange.ender_bags.recipe.DyeBagRecipe;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EnderBags.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnderBagsGameTests {
    private EnderBagsGameTests() {
    }

    @GameTest(
            template = "empty",
            timeoutTicks = 40)
    public static void storageDurabilityMigrationAndTransactions(GameTestHelper helper) {
        var level = helper.getLevel();

        testWoolRecipes(level, helper);
        helper.assertTrue(
                level.getRecipeManager()
                        .byKey(ResourceLocation.fromNamespaceAndPath(EnderBags.MOD_ID, "dye_ender_bag"))
                        .isPresent(),
                "The Ender Bag dye recipe was not loaded");

        ItemStack emptyBag = ModItems.ENDER_BAG.toStack();
        helper.assertTrue(emptyBag.getMaxStackSize() == 1, "An empty Ender Bag can stack");
        helper.assertTrue(!BagContents.hasStoredItems(emptyBag), "A new Ender Bag is not empty");
        helper.assertTrue(emptyBag.canFitInsideContainerItems(), "An empty Ender Bag cannot enter an item container");

        ItemEntity emptyLavaEntity = new ItemEntity(level, 0.0, 64.0, 0.0, emptyBag.copy());
        helper.assertTrue(emptyLavaEntity.lifespan == 6000, "An empty Ender Bag cannot despawn normally");
        helper.assertTrue(
                emptyLavaEntity.hurt(level.damageSources().lava(), Float.MAX_VALUE)
                        && emptyLavaEntity.isRemoved(),
                "Lava could not destroy an empty Ender Bag");

        ItemEntity emptyCactusEntity = new ItemEntity(level, 0.0, 64.0, 0.0, emptyBag.copy());
        helper.assertTrue(
                emptyCactusEntity.hurt(level.damageSources().cactus(), Float.MAX_VALUE)
                        && emptyCactusEntity.isRemoved(),
                "Cactus damage could not destroy an empty Ender Bag");

        ItemStack bag = ModItems.ENDER_BAG.toStack();
        NonNullList<ItemStack> contents = NonNullList.withSize(BagContents.SLOT_COUNT, ItemStack.EMPTY);
        contents.set(0, new ItemStack(Items.DIAMOND, 64));
        contents.set(BagContents.SLOT_COUNT - 1, new ItemStack(Items.ENDER_PEARL, 16));
        helper.assertTrue(BagContents.save(bag, contents), "A valid contents snapshot was rejected");

        // Both save and load must be detached snapshots; aliasing would enable duplication or loss.
        contents.get(0).setCount(1);
        NonNullList<ItemStack> loaded = requireContents(bag, level, helper);
        helper.assertTrue(
                loaded.get(0).is(Items.DIAMOND) && loaded.get(0).getCount() == 64,
                "Saving retained a mutable reference to the source inventory");
        loaded.get(0).setCount(2);
        helper.assertTrue(
                requireContents(bag, level, helper).get(0).getCount() == 64,
                "Loading exposed a mutable reference to persisted contents");

        helper.assertTrue(BagContents.hasStoredItems(bag), "A filled Ender Bag was reported as empty");
        helper.assertTrue(bag.getMaxStackSize() == 1, "A filled Ender Bag can stack");
        helper.assertTrue(bag.canFitInsideContainerItems(), "A filled Ender Bag cannot enter an item container");

        @SuppressWarnings("removal")
        Player mockPlayer = helper.makeMockServerPlayerInLevel();
        SimpleContainer chestInventory = new SimpleContainer(27);
        ChestMenu chestMenu = ChestMenu.threeRows(1, mockPlayer.getInventory(), chestInventory);
        helper.assertTrue(
                chestMenu.getSlot(0).mayPlace(bag),
                "An ordinary chest rejected a filled Ender Bag");

        mockPlayer.getInventory().selected = 0;
        ItemStack openedBag = ModItems.ENDER_BAG.toStack();
        mockPlayer.getInventory().setItem(0, openedBag);
        EnderBagMenu enderBagMenu = new EnderBagMenu(
                2,
                mockPlayer.getInventory(),
                0,
                openedBag);
        helper.assertTrue(enderBagMenu.stillValid(mockPlayer), "A valid Ender Bag menu was rejected");
        helper.assertTrue(
                !enderBagMenu.getSlot(0).mayPlace(emptyBag)
                        && !enderBagMenu.getSlot(0).mayPlace(bag),
                "An Ender Bag accepted another Ender Bag");

        // Shift-click in and back out. Each half must already be reflected in the held stack.
        mockPlayer.getInventory().setItem(9, new ItemStack(Items.GOLD_INGOT, 32));
        helper.assertTrue(
                !enderBagMenu.quickMoveStack(mockPlayer, BagContents.SLOT_COUNT).isEmpty(),
                "Shift-clicking into an Ender Bag failed");
        helper.assertTrue(
                mockPlayer.getInventory().getItem(9).isEmpty()
                        && requireContents(openedBag, level, helper).get(0).is(Items.GOLD_INGOT)
                        && requireContents(openedBag, level, helper).get(0).getCount() == 32,
                "The insert operation was not committed to the held bag");
        helper.assertTrue(
                !enderBagMenu.quickMoveStack(mockPlayer, 0).isEmpty(),
                "Shift-clicking out of an Ender Bag failed");
        helper.assertTrue(
                !BagContents.hasStoredItems(openedBag)
                        && countItem(mockPlayer, Items.GOLD_INGOT) == 32,
                "The extraction operation did not conserve its item count");

        // Replacing the selected stack invalidates the session; close must not overwrite it.
        ItemStack replacementBag = ModItems.ENDER_BAG.toStack();
        mockPlayer.getInventory().setItem(0, replacementBag);
        helper.assertTrue(!enderBagMenu.stillValid(mockPlayer), "A copied replacement bag kept the session alive");
        enderBagMenu.removed(mockPlayer);
        helper.assertTrue(
                mockPlayer.getInventory().getItem(0) == replacementBag
                        && !BagContents.hasStoredItems(replacementBag),
                "Closing a stale menu overwrote the replacement stack");

        // Vanilla close/disconnect handling must return the server-side cursor stack.
        mockPlayer.getInventory().setItem(0, openedBag);
        EnderBagMenu closeMenu = new EnderBagMenu(3, mockPlayer.getInventory(), 0, openedBag);
        closeMenu.setCarried(new ItemStack(Items.EMERALD, 7));
        closeMenu.removed(mockPlayer);
        helper.assertTrue(
                closeMenu.getCarried().isEmpty() && countItem(mockPlayer, Items.EMERALD) == 7,
                "Closing the menu lost its server-side cursor stack");

        DyeBagRecipe dyeRecipe = new DyeBagRecipe(CraftingBookCategory.MISC);
        CraftingInput dyeInput = CraftingInput.of(
                2,
                1,
                List.of(bag, new ItemStack(Items.RED_DYE)));
        helper.assertTrue(dyeRecipe.matches(dyeInput, level), "A bag and dye did not match the dye recipe");
        ItemStack redBag = dyeRecipe.assemble(dyeInput, level.registryAccess());
        DyedItemColor dyedColor = redBag.get(DataComponents.DYED_COLOR);
        helper.assertTrue(
                dyedColor != null
                        && (dyedColor.rgb() & 0xFFFFFF)
                                == (DyeColor.RED.getTextureDiffuseColor() & 0xFFFFFF),
                "Dyeing did not apply the requested color");
        helper.assertTrue(
                requireContents(redBag, level, helper).get(0).is(Items.DIAMOND),
                "Dyeing discarded the bag's contents");

        ItemEntity protectedEntity = new ItemEntity(level, 0.0, 64.0, 0.0, bag.copy());
        helper.assertTrue(
                !protectedEntity.hurt(level.damageSources().lava(), Float.MAX_VALUE),
                "Lava damage destroyed a filled Ender Bag");
        helper.assertTrue(
                !protectedEntity.hurt(level.damageSources().cactus(), Float.MAX_VALUE),
                "Cactus damage destroyed a filled Ender Bag");
        protectedEntity.tick();
        helper.assertTrue(
                protectedEntity.lifespan == Integer.MAX_VALUE && !protectedEntity.isRemoved(),
                "A filled Ender Bag can still despawn");
        helper.assertTrue(
                BagContents.save(
                        protectedEntity.getItem(),
                        NonNullList.withSize(BagContents.SLOT_COUNT, ItemStack.EMPTY)),
                "Could not empty the dropped Ender Bag");
        protectedEntity.tick();
        helper.assertTrue(
                protectedEntity.lifespan == 6000,
                "A dropped bag did not regain its normal lifespan after becoming empty");

        ItemEntity voidDamageEntity = new ItemEntity(level, 0.0, 64.0, 0.0, bag.copy());
        helper.assertTrue(
                voidDamageEntity.hurt(level.damageSources().fellOutOfWorld(), Float.MAX_VALUE)
                        && voidDamageEntity.isRemoved(),
                "Void damage did not destroy an Ender Bag");

        ItemEntity fallingEntity = new ItemEntity(
                level,
                0.0,
                level.getMinBuildHeight() - 65.0,
                0.0,
                bag.copy());
        fallingEntity.tick();
        helper.assertTrue(fallingEntity.isRemoved(), "An Ender Bag survived below the void threshold");

        // Pre-existing nested data is preserved for manual recovery, but the menu refuses new nesting.
        ItemStack nestingBag = ModItems.ENDER_BAG.toStack();
        NonNullList<ItemStack> nestedContents =
                NonNullList.withSize(BagContents.SLOT_COUNT, ItemStack.EMPTY);
        nestedContents.set(0, emptyBag.copy());
        helper.assertTrue(BagContents.save(nestingBag, nestedContents), "Could not create recovery test data");
        helper.assertTrue(
                requireContents(nestingBag, level, helper).get(0).is(ModItems.ENDER_BAG.get()),
                "Pre-existing nested bag data was silently deleted");

        ItemStack legacyBag = createLegacyBag();
        helper.assertTrue(BagContents.hasStoredItems(legacyBag), "A filled legacy bag lost protection");
        helper.assertTrue(
                BagContents.migrateLegacyData(legacyBag, level.registryAccess()),
                "A valid legacy bag could not be migrated");
        NonNullList<ItemStack> migrated = requireContents(legacyBag, level, helper);
        helper.assertTrue(
                migrated.get(BagContents.SLOT_COUNT - 1).is(Items.DIAMOND)
                        && migrated.get(BagContents.SLOT_COUNT - 1).getCount() == 64,
                "The legacy inventory contents were not migrated");
        helper.assertTrue(
                migrated.get(0).is(ModItems.ENDER_BAG.get()),
                "A pre-existing nested legacy bag was silently deleted");
        helper.assertTrue(!legacyBag.has(DataComponents.CUSTOM_DATA), "Migrated legacy data was not removed");
        helper.assertTrue(
                BagContents.migrateLegacyData(legacyBag, level.registryAccess()),
                "Legacy migration was not idempotent");

        ItemStack malformedLegacyBag = createMalformedLegacyBag();
        CustomData malformedBefore = malformedLegacyBag.get(DataComponents.CUSTOM_DATA);
        helper.assertTrue(
                BagContents.load(malformedLegacyBag, level.registryAccess()).isEmpty()
                        && !BagContents.migrateLegacyData(malformedLegacyBag, level.registryAccess()),
                "Conflicting legacy data was accepted");
        helper.assertTrue(
                malformedBefore != null
                        && malformedBefore.equals(malformedLegacyBag.get(DataComponents.CUSTOM_DATA))
                        && !malformedLegacyBag.has(DataComponents.CONTAINER),
                "Failed migration modified or deleted the original legacy data");

        ItemStack overflowBag = ModItems.ENDER_BAG.toStack();
        NonNullList<ItemStack> overflow =
                NonNullList.withSize(BagContents.SLOT_COUNT + 1, ItemStack.EMPTY);
        overflow.set(BagContents.SLOT_COUNT, new ItemStack(Items.NETHERITE_INGOT));
        ItemContainerContents overflowComponent = ItemContainerContents.fromItems(overflow);
        overflowBag.set(DataComponents.CONTAINER, overflowComponent);
        helper.assertTrue(
                BagContents.load(overflowBag, level.registryAccess()).isEmpty()
                        && !BagContents.save(
                                overflowBag,
                                NonNullList.withSize(BagContents.SLOT_COUNT, ItemStack.EMPTY))
                        && overflowComponent.equals(overflowBag.get(DataComponents.CONTAINER)),
                "Hidden overflow data was silently overwritten");

        helper.succeed();
    }

    private static void testWoolRecipes(
            net.minecraft.world.level.Level level,
            GameTestHelper helper) {
        for (DyeColor color : DyeColor.values()) {
            String path = color == DyeColor.WHITE
                    ? "ender_bag"
                    : "ender_bag_" + color.getName();
            ResourceLocation expectedId =
                    ResourceLocation.fromNamespaceAndPath(EnderBags.MOD_ID, path);
            helper.assertTrue(
                    level.getRecipeManager().byKey(expectedId).isPresent(),
                    "Missing Ender Bag crafting recipe for " + color.getName());

            CraftingInput input = bagCraftingInput(woolFor(color), woolFor(color));
            var matched = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            helper.assertTrue(
                    matched.isPresent() && matched.orElseThrow().id().equals(expectedId),
                    "Matching " + color.getName() + " wool did not select its bag recipe");

            ItemStack result = matched.orElseThrow().value().assemble(input, level.registryAccess());
            helper.assertTrue(
                    result.is(ModItems.ENDER_BAG.get())
                            && result.getCount() == 1
                            && BagItem.getDyeColor(result) == color,
                    "The " + color.getName() + " wool recipe produced the wrong bag color");
        }

        CraftingInput mixedInput = bagCraftingInput(Items.RED_WOOL, Items.BLUE_WOOL);
        helper.assertTrue(
                level.getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, mixedInput, level)
                        .isEmpty(),
                "Mixed wool colors unexpectedly formed an Ender Bag");
    }

    private static CraftingInput bagCraftingInput(Item primaryWool, Item differentBottomWool) {
        return CraftingInput.of(
                3,
                3,
                List.of(
                        new ItemStack(primaryWool),
                        new ItemStack(Items.STRING),
                        new ItemStack(primaryWool),
                        new ItemStack(primaryWool),
                        new ItemStack(Items.ENDER_CHEST),
                        new ItemStack(primaryWool),
                        new ItemStack(primaryWool),
                        new ItemStack(primaryWool),
                        new ItemStack(differentBottomWool)));
    }

    private static Item woolFor(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_WOOL;
            case ORANGE -> Items.ORANGE_WOOL;
            case MAGENTA -> Items.MAGENTA_WOOL;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_WOOL;
            case YELLOW -> Items.YELLOW_WOOL;
            case LIME -> Items.LIME_WOOL;
            case PINK -> Items.PINK_WOOL;
            case GRAY -> Items.GRAY_WOOL;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL;
            case CYAN -> Items.CYAN_WOOL;
            case PURPLE -> Items.PURPLE_WOOL;
            case BLUE -> Items.BLUE_WOOL;
            case BROWN -> Items.BROWN_WOOL;
            case GREEN -> Items.GREEN_WOOL;
            case RED -> Items.RED_WOOL;
            case BLACK -> Items.BLACK_WOOL;
        };
    }

    private static NonNullList<ItemStack> requireContents(
            ItemStack bag,
            net.minecraft.world.level.Level level,
            GameTestHelper helper) {
        var loaded = BagContents.load(bag, level.registryAccess());
        helper.assertTrue(loaded.isPresent(), "Valid Ender Bag contents could not be read");
        return loaded.orElseThrow();
    }

    private static int countItem(Player player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static ItemStack createLegacyBag() {
        ItemStack bag = ModItems.ENDER_BAG.toStack();
        CompoundTag root = new CompoundTag();
        CompoundTag inventory = new CompoundTag();
        inventory.putInt("Size", Integer.MAX_VALUE);

        ListTag items = new ListTag();
        items.add(createLegacyStack(0, "ender_bags:ender_bag", 1));
        items.add(createLegacyStack(BagContents.SLOT_COUNT - 1, "minecraft:diamond", 64));
        inventory.put("Items", items);
        root.put("inv", inventory);
        CustomData.set(DataComponents.CUSTOM_DATA, bag, root);
        return bag;
    }

    private static ItemStack createMalformedLegacyBag() {
        ItemStack bag = ModItems.ENDER_BAG.toStack();
        CompoundTag root = new CompoundTag();
        CompoundTag inventory = new CompoundTag();
        ListTag items = new ListTag();
        items.add(createLegacyStack(0, "minecraft:diamond", 1));
        items.add(createLegacyStack(0, "minecraft:emerald", 1));
        inventory.put("Items", items);
        root.put("inv", inventory);
        CustomData.set(DataComponents.CUSTOM_DATA, bag, root);
        return bag;
    }

    private static CompoundTag createLegacyStack(int slot, String itemId, int count) {
        CompoundTag stack = new CompoundTag();
        stack.putInt("Slot", slot);
        stack.putString("id", itemId);
        stack.putByte("Count", (byte)count);
        return stack;
    }
}
