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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AlarmBlockEntity extends BlockEntity implements IRedstoneLinkable {
    private ItemStack frequencyFirst = ItemStack.EMPTY;
    private ItemStack frequencyLast = ItemStack.EMPTY;
    private ResourceLocation alarmTypeId;
    private int receivedSignal = 0;
    private boolean registered = false;
    private long lastPoweredChangeTime = -1;
    private boolean wasPowered = false;

    public AlarmBlockEntity(BlockPos pos, BlockState blockState) {
        super(CreateAlerted.ALARM_BLOCK_ENTITY.get(), pos, blockState);
        this.alarmTypeId = AlarmTypeManager.INSTANCE.getDefaultId();
    }

    public ResourceLocation getTypeId() {
        return alarmTypeId;
    }

    public void setTypeId(ResourceLocation id) {
        this.alarmTypeId = id;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public AlarmType getAlarmType() {
        return AlarmTypeManager.INSTANCE.getType(alarmTypeId);
    }

    public void registerNetwork() {
        if (level != null && !level.isClientSide && !registered && hasFrequency()) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, this);
            registered = true;
        }
    }

    private boolean hasFrequency() {
        return !frequencyFirst.isEmpty() || !frequencyLast.isEmpty();
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
                    if (shouldBePowered) {
                        level.scheduleTick(worldPosition, state.getBlock(), 0);
                    }
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
            registered = false;
        }
        if (first) {
            frequencyFirst = stack.copy();
        } else {
            frequencyLast = stack.copy();
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            if (hasFrequency()) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, this);
                Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(level, this);
                registered = true;
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getReceivedSignal() {
        return receivedSignal;
    }

    public long getLastPoweredChangeTime() {
        return lastPoweredChangeTime;
    }

    public void setLastPoweredChangeTime(long time) {
        this.lastPoweredChangeTime = time;
    }

    public boolean wasPowered() {
        return wasPowered;
    }

    public void setWasPowered(boolean powered) {
        this.wasPowered = powered;
    }

    public SoundEvent getAlarmSound() {
        return AlarmTypeManager.getSound(alarmTypeId);
    }

    public int getSoundInterval() {
        return AlarmTypeManager.getInterval(alarmTypeId);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("FrequencyFirst", frequencyFirst.saveOptional(registries));
        tag.put("FrequencyLast", frequencyLast.saveOptional(registries));
        tag.putString("AlarmType", alarmTypeId.toString());
        tag.putInt("ReceivedSignal", receivedSignal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        frequencyFirst = ItemStack.parseOptional(registries, tag.getCompound("FrequencyFirst"));
        frequencyLast = ItemStack.parseOptional(registries, tag.getCompound("FrequencyLast"));
        alarmTypeId = ResourceLocation.parse(tag.getString("AlarmType"));
        receivedSignal = tag.getInt("ReceivedSignal");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("FrequencyFirst", frequencyFirst.saveOptional(registries));
        tag.put("FrequencyLast", frequencyLast.saveOptional(registries));
        tag.putString("AlarmType", alarmTypeId.toString());
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
