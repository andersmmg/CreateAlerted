package com.andersmmg.create_alerted.menu;

import com.andersmmg.create_alerted.CreateAlerted;
import com.andersmmg.create_alerted.block.AlarmBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AlarmMenu extends AbstractContainerMenu {
    private static final Container DUMMY_CONTAINER = new SimpleContainer(1);
    private final AlarmBlockEntity blockEntity;

    public AlarmMenu(int containerId, Inventory playerInventory, AlarmBlockEntity blockEntity) {
        super(CreateAlerted.ALARM_MENU.get(), containerId);
        this.blockEntity = blockEntity;

        addSlot(new FrequencySlot(blockEntity, true, 16, 27));
        addSlot(new FrequencySlot(blockEntity, false, 36, 27));

        addPlayerSlots(playerInventory, 28, 106);
    }

    public static AlarmMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        var pos = buf.readBlockPos();
        var level = playerInventory.player.level();
        if (level.getBlockEntity(pos) instanceof AlarmBlockEntity be) {
            return new AlarmMenu(containerId, playerInventory, be);
        }
        return null;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId == 0 || slotId == 1) {
            if (clickType == ClickType.PICKUP) {
                boolean isFirst = (slotId == 0);
                ItemStack held = getCarried();
                blockEntity.setFrequency(isFirst, held.isEmpty() ? ItemStack.EMPTY : held.copyWithCount(1));
            }
            return;
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    private void addPlayerSlots(Inventory inv, int x, int y) {
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, x + col * 18, y + row * 18));
        for (int hotbarSlot = 0; hotbarSlot < 9; ++hotbarSlot)
            addSlot(new Slot(inv, hotbarSlot, x + hotbarSlot * 18, y + 58));
    }

    public AlarmBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private static class FrequencySlot extends Slot {
        private final AlarmBlockEntity blockEntity;
        private final boolean first;

        public FrequencySlot(AlarmBlockEntity blockEntity, boolean first, int x, int y) {
            super(DUMMY_CONTAINER, 0, x, y);
            this.blockEntity = blockEntity;
            this.first = first;
        }

        @Override
        public void setChanged() {
        }

        @Override
        public @NotNull ItemStack getItem() {
            return first ? blockEntity.getFrequencyFirst() : blockEntity.getFrequencyLast();
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }

        @Override
        public void setByPlayer(@NotNull ItemStack stack) {
        }

        @Override
        public void set(@NotNull ItemStack stack) {
        }

        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return false;
        }

        @Override
        public @NotNull ItemStack remove(int amount) {
            return ItemStack.EMPTY;
        }
    }
}
