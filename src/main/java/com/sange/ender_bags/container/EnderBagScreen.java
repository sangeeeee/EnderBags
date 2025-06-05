
package com.sange.ender_bags.container;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sange.ender_bags.EnderBags;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("removal")
public class EnderBagScreen extends AbstractContainerScreen<EnderBagMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(EnderBags.MOD_ID, "textures/gui/bag.png");

    public EnderBagScreen(EnderBagMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256; // Adjust based on bag.png width
        this.imageHeight = 256; // Adjust based on bag.png height
        this.inventoryLabelX = 48;
        this.inventoryLabelY = 141;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }


    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Skip rendering titles for "Ender Bag" and "Inventory"
    }
}