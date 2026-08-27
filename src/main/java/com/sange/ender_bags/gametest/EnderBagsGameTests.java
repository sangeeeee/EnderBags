package com.sange.ender_bags.gametest;

import com.sange.ender_bags.EnderBags;
import com.sange.ender_bags.item.BagContents;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
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
    public static void storageDurabilityAndMigration(GameTestHelper helper) {
        var level = helper.getLevel();

        helper.assertTrue(
                level.getRecipeManager()
                        .byKey(ResourceLocation.fromNamespaceAndPath(EnderBags.MOD_ID, "ender_bag"))
                        .isPresent(),
                "The Ender Bag crafting recipe was not loaded");
        helper.assertTrue(
                level.getRecipeManager()
                        .byKey(ResourceLocation.fromNamespaceAndPath(EnderBags.MOD_ID, "dye_ender_bag"))
                        .isPresent(),
                "The Ender Bag dye recipe was not loaded");

        ItemStack bag = ModItems.ENDER_BAG.toStack();
        NonNullList<ItemStack> contents = NonNullList.withSize(BagContents.SLOT_COUNT, ItemStack.EMPTY);
        contents.set(0, new ItemStack(Items.DIAMOND, 64));
        contents.set(BagContents.SLOT_COUNT - 1, new ItemStack(Items.ENDER_PEARL, 16));
        BagContents.save(bag, contents);

        NonNullList<ItemStack> loaded = BagContents.load(bag, level.registryAccess());
        helper.assertTrue(loaded.size() == BagContents.SLOT_COUNT, "The bag did not retain its fixed slot count");
        helper.assertTrue(loaded.get(0).is(Items.DIAMOND) && loaded.get(0).getCount() == 64,
                "The first stored stack was corrupted");
        helper.assertTrue(
                loaded.get(BagContents.SLOT_COUNT - 1).is(Items.ENDER_PEARL)
                        && loaded.get(BagContents.SLOT_COUNT - 1).getCount() == 16,
                "The last stored stack was corrupted");

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
                BagContents.load(redBag, level.registryAccess()).get(0).is(Items.DIAMOND),
                "Dyeing discarded the bag's contents");

        ItemEntity protectedEntity = new ItemEntity(level, 0.0, 64.0, 0.0, bag.copy());
        helper.assertTrue(
                !protectedEntity.hurt(level.damageSources().lava(), Float.MAX_VALUE),
                "Lava damage destroyed an Ender Bag");
        helper.assertTrue(
                !protectedEntity.hurt(level.damageSources().cactus(), Float.MAX_VALUE),
                "Cactus damage destroyed an Ender Bag");
        protectedEntity.tick();
        helper.assertTrue(protectedEntity.getAge() == -32768, "A dropped Ender Bag can still despawn");

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

        ItemStack legacyBag = createLegacyBag();
        NonNullList<ItemStack> migrated = BagContents.load(legacyBag, level.registryAccess());
        helper.assertTrue(migrated.size() == BagContents.SLOT_COUNT,
                "A hostile legacy Size value changed the bag capacity");
        helper.assertTrue(
                migrated.get(BagContents.SLOT_COUNT - 1).is(Items.DIAMOND)
                        && migrated.get(BagContents.SLOT_COUNT - 1).getCount() == 64,
                "The legacy inventory contents were not migrated");
        helper.assertTrue(migrated.get(0).isEmpty(), "A nested legacy Ender Bag was accepted");
        helper.assertTrue(!legacyBag.has(DataComponents.CUSTOM_DATA), "Migrated legacy data was not removed");

        helper.succeed();
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

    private static CompoundTag createLegacyStack(int slot, String itemId, int count) {
        CompoundTag stack = new CompoundTag();
        stack.putInt("Slot", slot);
        stack.putString("id", itemId);
        stack.putByte("Count", (byte) count);
        return stack;
    }
}
