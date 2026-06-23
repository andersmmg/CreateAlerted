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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SmokeDetectorBlockEntity extends SmartBlockEntity {
    public static final int DETECT_INTERVAL = 10;
    public static final int SOUND_INTERVAL = 80;

    private static final Map<Integer, SphereOffsets> SPHERE_OFFSETS = new ConcurrentHashMap<>();
    private long lastDetectTime = -1;

    private long lastSoundTickTime = -1;

    private static SphereOffsets buildSphereTable(int radius) {
        int r2 = radius * radius;
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            int dx2 = dx * dx;
            for (int dy = -radius; dy <= radius; dy++) {
                int sum2 = dx2 + dy * dy;
                if (sum2 > r2) continue;
                int dzMax = (int) Math.sqrt(r2 - sum2);
                count += 2 * dzMax + 1;
            }
        }
        int[] dxArr = new int[count];
        int[] dyArr = new int[count];
        int[] dzArr = new int[count];
        int idx = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            int dx2 = dx * dx;
            for (int dy = -radius; dy <= radius; dy++) {
                int sum2 = dx2 + dy * dy;
                if (sum2 > r2) continue;
                int dzMax = (int) Math.sqrt(r2 - sum2);
                for (int dz = -dzMax; dz <= dzMax; dz++) {
                    dxArr[idx] = dx;
                    dyArr[idx] = dy;
                    dzArr[idx] = dz;
                    idx++;
                }
            }
        }
        return new SphereOffsets(dxArr, dyArr, dzArr);
    }
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

    public void silence() {
        if (level == null || level.isClientSide) return;

        silenced = true;
        BlockState state = getBlockState();
        if (state.getValue(SmokeDetectorBlock.POWERED)) {
            BlockState newState = state.setValue(SmokeDetectorBlock.POWERED, false);
            level.setBlock(worldPosition, newState, 3);
            level.updateNeighborsAt(worldPosition, CreateAlerted.SMOKE_DETECTOR_BLOCK.get());
        }
        setChanged();
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        long gameTime = level.getGameTime();
        BlockState state = getBlockState();
        boolean powered = state.getValue(SmokeDetectorBlock.POWERED);

        if (lastDetectTime < 0 || gameTime < lastDetectTime || gameTime - lastDetectTime >= DETECT_INTERVAL) {
            boolean fireDetected = checkForFire();
            lastDetectTime = gameTime;

            if (silenced && !fireDetected) {
                silenced = false;
            }

            if (!silenced && fireDetected && !powered) {
                BlockState newState = state.setValue(SmokeDetectorBlock.POWERED, true);
                level.setBlock(worldPosition, newState, 3);
                level.updateNeighborsAt(worldPosition, CreateAlerted.SMOKE_DETECTOR_BLOCK.get());
                state = newState;
                powered = true;
                lastSoundTickTime = -1;
                setChanged();
            }
        }

        if (powered && (lastSoundTickTime < 0 || gameTime < lastSoundTickTime || gameTime - lastSoundTickTime >= SOUND_INTERVAL)) {
            SmokeDetectorBlock.playSound(level, worldPosition, AllSoundEvents.SMOKE_DETECTOR.get(), (float) Config.alarmVolume * 2.0f);
            lastSoundTickTime = gameTime;
        }
    }

    private boolean checkForFire() {
        int radius = Config.smokeDetectorRadius;
        if (radius <= 0) return false;
        SphereOffsets offsets = SPHERE_OFFSETS.computeIfAbsent(radius, SmokeDetectorBlockEntity::buildSphereTable);
        int x0 = worldPosition.getX();
        int y0 = worldPosition.getY();
        int z0 = worldPosition.getZ();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        int[] dxArr = offsets.dx();
        int[] dyArr = offsets.dy();
        int[] dzArr = offsets.dz();
        int count = dxArr.length;
        for (int i = 0; i < count; i++) {
            mutablePos.set(x0 + dxArr[i], y0 + dyArr[i], z0 + dzArr[i]);
            if (level.getBlockState(mutablePos).is(BlockTags.FIRE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putLong("LastSoundTick", lastSoundTickTime);
        tag.putLong("LastDetectTick", lastDetectTime);
        tag.putBoolean("Silenced", silenced);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        lastSoundTickTime = tag.getLong("LastSoundTick");
        lastDetectTime = tag.contains("LastDetectTick") ? tag.getLong("LastDetectTick") : -1L;
        silenced = tag.getBoolean("Silenced");
    }

    private record SphereOffsets(int[] dx, int[] dy, int[] dz) {
    }
}
