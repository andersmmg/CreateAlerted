package com.andersmmg.create_alerted.block;

import com.andersmmg.create_alerted.CreateAlerted;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Couple;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AlarmBlockEntity extends SmartBlockEntity implements IRedstoneLinkable, ClipboardCloneable {

    // Timing constants matching the renderer (in ticks, 20 ticks = 1 second)
    public static final float BLINK_FADE_SECONDS = 0.2f;
    public static final float BLINK_ON_SECONDS = 1.0f;
    public static final float BLINK_OFF_SECONDS = 1.0f;
    public static final float BLINK_FADE_TICKS = BLINK_FADE_SECONDS * 20.0f;
    public static final float BLINK_ON_TICKS = BLINK_ON_SECONDS * 20.0f;
    public static final float BLINK_FADE_END_TICKS = BLINK_FADE_TICKS + BLINK_ON_TICKS;
    public static final float BLINK_FADE_OUT_END_TICKS = BLINK_FADE_END_TICKS + BLINK_FADE_TICKS;
    public static final float BLINK_OFF_TICKS = BLINK_OFF_SECONDS * 20.0f;
    public static final float BLINK_CYCLE_TICKS = BLINK_FADE_OUT_END_TICKS + BLINK_OFF_TICKS;

    public static final float FLASH_ON_SECONDS = 0.3f;
    public static final float FLASH_OFF_SECONDS = 1.0f;
    public static final float FLASH_FADE_TICKS = 2f;
    public static final float FLASH_ON_TICKS = FLASH_ON_SECONDS * 20.0f;
    public static final float FLASH_ON_PLATEAU_END = FLASH_ON_TICKS - FLASH_FADE_TICKS;
    public static final float FLASH_OFF_TICKS = FLASH_OFF_SECONDS * 20.0f;
    public static final float FLASH_CYCLE_TICKS = FLASH_ON_TICKS + FLASH_OFF_TICKS;
    private ItemStack frequencyFirst = ItemStack.EMPTY;
    private ItemStack frequencyLast = ItemStack.EMPTY;
    private ResourceLocation alarmTypeId;
    private ResourceLocation visualId = AlarmVisualType.SPIN.id();
    private int receivedSignal = 0;
    private boolean registered = false;
    private long lastPoweredChangeTime = -1;
    private boolean wasPowered = false;
    private int color = DyeColor.RED.getTextureDiffuseColor() & 0xFFFFFF;

    public AlarmBlockEntity(BlockPos pos, BlockState blockState) {
        super(CreateAlerted.ALARM_BLOCK_ENTITY.get(), pos, blockState);
        this.alarmTypeId = AlarmTypeManager.INSTANCE.getDefaultId();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public ResourceLocation getTypeId() {
        return alarmTypeId;
    }

    public void setTypeId(ResourceLocation id) {
        this.alarmTypeId = id;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
            if (state.getBlock() instanceof AlarmBlock && state.getValue(AlarmBlock.POWERED) && getAlarmSound() != null) {
                level.scheduleTick(worldPosition, state.getBlock(), 0);
            }
        }
    }

    public ResourceLocation getVisualId() {
        return visualId;
    }

    public void setVisualId(ResourceLocation id) {
        this.visualId = id;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isLightCycling() {
        if (level == null || level.isClientSide) return false;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof AlarmBlock) || !state.getValue(AlarmBlock.POWERED)) return false;
        AlarmVisualType visual = AlarmVisualType.byId(visualId);
        return visual == AlarmVisualType.FLASHING || visual == AlarmVisualType.BLINK;
    }

    public boolean shouldLightBeOn(long gameTime) {
        AlarmVisualType visual = AlarmVisualType.byId(visualId);
        return switch (visual) {
            case FLASHING -> (gameTime % (long) FLASH_CYCLE_TICKS) < (long) FLASH_ON_TICKS;
            case BLINK -> (gameTime % (long) BLINK_CYCLE_TICKS) < (long) BLINK_FADE_OUT_END_TICKS;
            default -> true;
        };
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
    public void invalidate() {
        if (level != null && !level.isClientSide && registered) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, this);
            registered = false;
        }
        super.invalidate();
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
                    BlockState newState = state.setValue(AlarmBlock.POWERED, shouldBePowered);
                    if (shouldBePowered) {
                        newState = newState.setValue(AlarmBlock.LIGHT, shouldLightBeOn(level.getGameTime()));
                    } else {
                        newState = newState.setValue(AlarmBlock.LIGHT, false);
                    }
                    level.setBlock(worldPosition, newState, 3);
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

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
        if (level != null && level.isClientSide) {
            Minecraft.getInstance().levelRenderer
                    .setSectionDirty(worldPosition.getX() >> 4, worldPosition.getY() >> 4, worldPosition.getZ() >> 4);
        }
    }

    @Override
    public String getClipboardKey() {
        return "Frequencies";
    }

    @Override
    public boolean writeToClipboard(HolderLookup.Provider registries, CompoundTag tag, Direction side) {
        tag.put("First", frequencyFirst.saveOptional(registries));
        tag.put("Last", frequencyLast.saveOptional(registries));
        return true;
    }

    @Override
    public boolean readFromClipboard(HolderLookup.Provider registries, CompoundTag tag, Player player, Direction side, boolean simulate) {
        if (!tag.contains("First") || !tag.contains("Last"))
            return false;
        if (simulate)
            return true;
        setFrequency(true, ItemStack.parseOptional(registries, tag.getCompound("First")));
        setFrequency(false, ItemStack.parseOptional(registries, tag.getCompound("Last")));
        return true;
    }

    public @Nullable SoundEvent getAlarmSound() {
        return AlarmTypeManager.getSound(alarmTypeId);
    }

    public @Nullable Integer getSoundInterval() {
        return AlarmTypeManager.getInterval(alarmTypeId);
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("FrequencyFirst", frequencyFirst.saveOptional(registries));
        tag.put("FrequencyLast", frequencyLast.saveOptional(registries));
        tag.putString("AlarmType", alarmTypeId.toString());
        tag.putString("AlarmVisual", visualId.toString());
        tag.putInt("ReceivedSignal", receivedSignal);
        tag.putInt("Color", color);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        frequencyFirst = ItemStack.parseOptional(registries, tag.getCompound("FrequencyFirst"));
        frequencyLast = ItemStack.parseOptional(registries, tag.getCompound("FrequencyLast"));
        alarmTypeId = ResourceLocation.parse(tag.getString("AlarmType"));
        visualId = tag.contains("AlarmVisual") ? ResourceLocation.parse(tag.getString("AlarmVisual")) : AlarmVisualType.SPIN.id();
        receivedSignal = tag.getInt("ReceivedSignal");
        color = tag.getInt("Color");
    }

}
