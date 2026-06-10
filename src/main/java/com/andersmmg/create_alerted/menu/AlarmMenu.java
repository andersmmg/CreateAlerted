package com.andersmmg.create_alerted.menu;

import com.andersmmg.create_alerted.Create_alerted;
import com.andersmmg.create_alerted.block.AlarmBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AlarmMenu extends AbstractContainerMenu {
    private static final Container DUMMY_CONTAINER = new SimpleContainer(1);
    private final AlarmBlockEntity blockEntity;

    public AlarmMenu(int containerId, Inventory playerInventory, AlarmBlockEntity blockEntity) {
        super(Create_alerted.ALARM_MENU.get(), containerId);
        this.blockEntity = blockEntity;

        addSlot(new FrequencySlot(blockEntity, true, 71, 20));
        addSlot(new FrequencySlot(blockEntity, false, 89, 20));

        addPlayerInventory(playerInventory, 51);
    }

    public static AlarmMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        var pos = buf.readBlockPos();
        var level = playerInventory.player.level();
        if (level.getBlockEntity(pos) instanceof AlarmBlockEntity be) {
            return new AlarmMenu(containerId, playerInventory, be);
        }
        return null;
    }

    private void addPlayerInventory(Inventory playerInventory, int yStart) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, yStart + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, yStart + 58));
        }
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
            return true;
        }

        @Override
        public void setByPlayer(@NotNull ItemStack stack) {
            blockEntity.setFrequency(first, stack);
        }

        @Override
        public void set(@NotNull ItemStack stack) {
            blockEntity.setFrequency(first, stack);
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
