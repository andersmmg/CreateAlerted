package com.andersmmg.create_alerted.screen;

import com.andersmmg.create_alerted.CreateAlerted;
import com.andersmmg.create_alerted.block.AlarmTypeManager;
import com.andersmmg.create_alerted.menu.AlarmMenu;
import com.andersmmg.create_alerted.network.AlarmTypePayload;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class AlarmScreen extends AbstractSimiContainerScreen<AlarmMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CreateAlerted.MODID, "textures/gui/alarm.png");
    private static final int BG_WIDTH = 216;
    private static final int BG_HEIGHT = 84;

    private ScrollInput typeScroll;
    private Label typeLabel;

    public AlarmScreen(AlarmMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        setWindowSize(BG_WIDTH, BG_HEIGHT + 4 + 108);
        setWindowOffset(0, 0);
        super.init();

        int x = leftPos;
        int y = topPos;

        List<ResourceLocation> order = AlarmTypeManager.INSTANCE.getOrder();
        if (order.isEmpty()) return;

        List<? extends Component> options = order.stream()
                .map(id -> Component.translatable(AlarmTypeManager.translationKey(id)))
                .toList();

        typeLabel = new Label(x + 63, y + 31, Component.empty())
                .colored(0x404040);

        typeScroll = new SelectionScrollInput(x + 60, y + 28, 142, 15)
                .forOptions(options)
                .titled(Component.literal("Alarm Type"))
                .writingTo(typeLabel)
                .calling(state -> {
                    ResourceLocation id = order.get(state);
                    menu.getBlockEntity().setTypeId(id);
                    PacketDistributor.sendToServer(
                            new AlarmTypePayload(menu.getBlockEntity().getBlockPos(), id));
                });

        int currentIdx = order.indexOf(menu.getBlockEntity().getTypeId());
        if (currentIdx >= 0) {
            typeScroll.setState(currentIdx);
        }

        IconButton confirmButton = new IconButton(x + BG_WIDTH - 24, y + 61, AllIcons.I_CONFIRM);
        confirmButton.withCallback(() -> {
            if (getMinecraft().player != null) {
                getMinecraft().player.closeContainer();
            }
        });

        addRenderableWidget(typeScroll);
        addRenderableWidget(typeLabel);
        addRenderableWidget(confirmButton);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.blit(TEXTURE, x, y, 0, 0, BG_WIDTH, BG_HEIGHT, 256, 256);

        Component titleText = Component.translatable("block.create_alerted.alarm");
        graphics.drawString(font, titleText, x + (BG_WIDTH - font.width(titleText)) / 2, y + 4, 0x404040, false);

        int invX = getLeftOfCentered(176);
        int invY = y + BG_HEIGHT + 4;
        renderPlayerInventory(graphics, invX, invY);
    }
}
