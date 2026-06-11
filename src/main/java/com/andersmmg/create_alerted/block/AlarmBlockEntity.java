package com.andersmmg.create_alerted.block;

import com.andersmmg.create_alerted.CreateAlerted;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AlarmBlockEntity extends BlockEntity implements IRedstoneLinkable {
    private ItemStack frequencyFirst = ItemStack.EMPTY;
    private ItemStack frequencyLast = ItemStack.EMPTY;
    private int receivedSignal = 0;
    private boolean registered = false;

    public AlarmBlockEntity(BlockPos pos, BlockState blockState) {
        super(CreateAlerted.ALARM_BLOCK_ENTITY.get(), pos, blockState);
    }

    public void registerNetwork() {
        if (level != null && !level.isClientSide && !registered) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, this);
            registered = true;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        registerNetwork();
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide && registered) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, this);
            registered = false;
        }
        super.setRemoved();
    }

    @Override
    public int getTransmittedStrength() {
        return 0;
    }

    @Override
    public void setReceivedStrength(int power) {
        this.receivedSignal = power;
        setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.getBlock() instanceof AlarmBlock) {
                boolean shouldBePowered = power > 0 || level.hasNeighborSignal(worldPosition);
                boolean currentlyPowered = state.getValue(AlarmBlock.POWERED);
                if (shouldBePowered != currentlyPowered) {
                    level.setBlock(worldPosition, state.setValue(AlarmBlock.POWERED, shouldBePowered), 3);
                }
            }
        }
    }

    @Override
    public boolean isListening() {
        return true;
    }

    @Override
    public boolean isAlive() {
        return level != null && level.getBlockEntity(worldPosition) == this && !isRemoved();
    }

    @Override
    public Couple<RedstoneLinkNetworkHandler.Frequency> getNetworkKey() {
        return Couple.create(
                RedstoneLinkNetworkHandler.Frequency.of(frequencyFirst),
                RedstoneLinkNetworkHandler.Frequency.of(frequencyLast)
        );
    }

    @Override
    public BlockPos getLocation() {
        return worldPosition;
    }

    public ItemStack getFrequencyFirst() {
        return frequencyFirst;
    }

    public ItemStack getFrequencyLast() {
        return frequencyLast;
    }

    public void setFrequency(boolean first, ItemStack stack) {
        if (level != null && !level.isClientSide && registered) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, this);
        }
        if (first) {
            frequencyFirst = stack.copy();
        } else {
            frequencyLast = stack.copy();
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, this);
            Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(level, this);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getReceivedSignal() {
        return receivedSignal;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("FrequencyFirst", frequencyFirst.saveOptional(registries));
        tag.put("FrequencyLast", frequencyLast.saveOptional(registries));
        tag.putInt("ReceivedSignal", receivedSignal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        frequencyFirst = ItemStack.parseOptional(registries, tag.getCompound("FrequencyFirst"));
        frequencyLast = ItemStack.parseOptional(registries, tag.getCompound("FrequencyLast"));
        receivedSignal = tag.getInt("ReceivedSignal");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("FrequencyFirst", frequencyFirst.saveOptional(registries));
        tag.put("FrequencyLast", frequencyLast.saveOptional(registries));
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
