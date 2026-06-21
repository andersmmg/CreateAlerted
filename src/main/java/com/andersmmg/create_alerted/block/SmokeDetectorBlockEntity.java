package com.andersmmg.create_alerted.block;

import com.andersmmg.create_alerted.AllSoundEvents;
import com.andersmmg.create_alerted.Config;
import com.andersmmg.create_alerted.CreateAlerted;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SmokeDetectorBlockEntity extends SmartBlockEntity {
    public static final int DETECT_INTERVAL = 10;
    public static final int SOUND_INTERVAL = 80;

    private long lastSoundTickTime = -1;
    private boolean silenced = false;

    public SmokeDetectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(CreateAlerted.SMOKE_DETECTOR_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public boolean isPowered() {
        BlockState state = getBlockState();
        return state.hasProperty(SmokeDetectorBlock.POWERED) && state.getValue(SmokeDetectorBlock.POWERED);
    }

    /**
     * Manually acknowledge/silence a triggered detector. The detector stays hushed while
     * fire is still present, and rearms once no fire is detected.
     */
    public void silence() {
        if (level == null || level.isClientSide) return;

        silenced = true;
        BlockState state = getBlockState();
        if (state.getValue(SmokeDetectorBlock.POWERED)) {
            BlockState newState = state.setValue(SmokeDetectorBlock.POWERED, false);
            level.setBlock(worldPosition, newState, 3);
            // Propagate the redstone change to neighbors
            level.updateNeighborsAt(worldPosition, CreateAlerted.SMOKE_DETECTOR_BLOCK.get());
        }
        setChanged();
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        long gameTime = level.getGameTime();
        boolean fireDetected = checkForFire();
        BlockState state = getBlockState();

        // If we're hushed and the fire has cleared, re-arm so the next fire re-triggers
        if (silenced && !fireDetected) {
            silenced = false;
        }

        // Fire alarm is latched: it stays powered until the player resets it. Only fire
        // when not already powered and not hushed.
        if (!silenced && fireDetected && !state.getValue(SmokeDetectorBlock.POWERED)) {
            BlockState newState = state.setValue(SmokeDetectorBlock.POWERED, true);
            level.setBlock(worldPosition, newState, 3);
            level.updateNeighborsAt(worldPosition, CreateAlerted.SMOKE_DETECTOR_BLOCK.get());
            state = newState;
            lastSoundTickTime = -1; // play immediately on first trigger
            setChanged();
        }

        if (state.getValue(SmokeDetectorBlock.POWERED)) {
            if (lastSoundTickTime < 0 || gameTime - lastSoundTickTime >= SOUND_INTERVAL) {
                SmokeDetectorBlock.playSound(level, worldPosition, AllSoundEvents.SMOKE_DETECTOR.get(), (float) Config.alarmVolume);
                lastSoundTickTime = gameTime;
            }
        }

        // Re-schedule detection tick
        level.scheduleTick(worldPosition, state.getBlock(), DETECT_INTERVAL);
    }

    private boolean checkForFire() {
        int radius = Config.smokeDetectorRadius;
        if (radius <= 0) return false;
        int x0 = worldPosition.getX();
        int y0 = worldPosition.getY();
        int z0 = worldPosition.getZ();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) continue;
                    mutablePos.set(x0 + dx, y0 + dy, z0 + dz);
                    BlockState state = level.getBlockState(mutablePos);
                    if (state.is(BlockTags.FIRE)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putLong("LastSoundTick", lastSoundTickTime);
        tag.putBoolean("Silenced", silenced);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        lastSoundTickTime = tag.getLong("LastSoundTick");
        silenced = tag.getBoolean("Silenced");
    }
}
