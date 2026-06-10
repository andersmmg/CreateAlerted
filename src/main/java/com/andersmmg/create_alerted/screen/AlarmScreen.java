package com.andersmmg.create_alerted.screen;

import com.andersmmg.create_alerted.Create_alerted;
import com.andersmmg.create_alerted.menu.AlarmMenu;
import com.andersmmg.create_alerted.network.AlarmFrequencyPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public class AlarmScreen extends AbstractContainerScreen<AlarmMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Create_alerted.MODID, "textures/gui/alarm.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 133;

    public AlarmScreen(AlarmMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
//        this.titleLabelY = 10000;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouse_x, int mouse_y, float p_281886_) {
        super.render(guiGraphics, mouse_x, mouse_y, p_281886_);
        super.renderTooltip(guiGraphics, mouse_x, mouse_y);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void slotClicked(Slot slot, int slotIndex, int mouseButton, ClickType type) {
        if (slotIndex >= 0 && slotIndex <= 1 && type == ClickType.PICKUP) {
            var cursorStack = getMinecraft().player.containerMenu.getCarried();
            boolean isFirst = (slotIndex == 0);

            if (cursorStack.isEmpty()) {
                menu.getBlockEntity().setFrequency(isFirst, ItemStack.EMPTY);
                PacketDistributor.sendToServer(
                        new AlarmFrequencyPayload(menu.getBlockEntity().getBlockPos(), isFirst, ItemStack.EMPTY)
                );
            } else {
                ItemStack freqStack = cursorStack.copyWithCount(1);
                menu.getBlockEntity().setFrequency(isFirst, freqStack);
                PacketDistributor.sendToServer(
                        new AlarmFrequencyPayload(menu.getBlockEntity().getBlockPos(), isFirst, freqStack)
                );
            }
            return;
        }
        super.slotClicked(slot, slotIndex, mouseButton, type);
    }
}
