package com.andersmmg.create_alerted.screen;

import com.andersmmg.create_alerted.CreateAlerted;
import com.andersmmg.create_alerted.block.AlarmType;
import com.andersmmg.create_alerted.block.AlarmTypeManager;
import com.andersmmg.create_alerted.menu.AlarmMenu;
import com.andersmmg.create_alerted.network.AlarmFrequencyPayload;
import com.andersmmg.create_alerted.network.AlarmTypePayload;
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

import java.util.List;

public class AlarmScreen extends AbstractContainerScreen<AlarmMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CreateAlerted.MODID, "textures/gui/alarm.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 133;

    private static final int TYPE_BUTTON_Y = 5;
    private static final int TYPE_BUTTON_WIDTH = 66;
    private static final int TYPE_BUTTON_HEIGHT = 14;
    private static final int TYPE_BUTTON_X = (WIDTH - TYPE_BUTTON_WIDTH) / 2;

    public AlarmScreen(AlarmMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 10000;
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

        int btnX = x + TYPE_BUTTON_X;
        int btnY = y + TYPE_BUTTON_Y;

        guiGraphics.fill(btnX, btnY, btnX + TYPE_BUTTON_WIDTH, btnY + TYPE_BUTTON_HEIGHT, 0xFF8B8B8B);
        guiGraphics.fill(btnX + 1, btnY + 1, btnX + TYPE_BUTTON_WIDTH - 1, btnY + TYPE_BUTTON_HEIGHT - 1, 0xFFC6C6C6);

        AlarmType currentType = menu.getBlockEntity().getAlarmType();
        Component typeText = currentType != null ? Component.translatable(AlarmTypeManager.translationKey(menu.getBlockEntity().getTypeId())) : Component.literal("?");
        int textWidth = this.font.width(typeText);
        int textX = btnX + (TYPE_BUTTON_WIDTH - textWidth) / 2;
        int textY = btnY + (TYPE_BUTTON_HEIGHT - 8) / 2;
        guiGraphics.drawString(this.font, typeText, textX, textY, 0xFF404040, false);
    }

    @Override
    protected void slotClicked(@NotNull Slot slot, int slotIndex, int mouseButton, @NotNull ClickType type) {
        if (slotIndex >= 0 && slotIndex <= 1 && type == ClickType.PICKUP) {
            assert getMinecraft().player != null;
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int btnX = x + TYPE_BUTTON_X;
        int btnY = y + TYPE_BUTTON_Y;

        if (mouseX >= btnX && mouseX < btnX + TYPE_BUTTON_WIDTH && mouseY >= btnY && mouseY < btnY + TYPE_BUTTON_HEIGHT) {
            List<ResourceLocation> order = AlarmTypeManager.INSTANCE.getOrder();
            if (!order.isEmpty()) {
                ResourceLocation currentId = menu.getBlockEntity().getTypeId();
                int idx = order.indexOf(currentId);
                ResourceLocation nextId = order.get((idx + 1) % order.size());
                menu.getBlockEntity().setTypeId(nextId);
                PacketDistributor.sendToServer(
                        new AlarmTypePayload(menu.getBlockEntity().getBlockPos(), nextId)
                );
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
