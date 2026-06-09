package com.andersmmg.create_alerted.block;

import com.andersmmg.create_alerted.AllSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class AnnoyingAlarmBlock extends AlarmBlock {
    public AnnoyingAlarmBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public SoundEvent getAlarmSound() {
        return AllSoundEvents.ALARM_ANNOYING.get();
    }

    @Override
    public int getSoundInterval() {
        return 20;
    }
}
