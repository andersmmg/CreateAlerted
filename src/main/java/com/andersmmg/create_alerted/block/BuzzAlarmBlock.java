package com.andersmmg.create_alerted.block;

import com.andersmmg.create_alerted.AllSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BuzzAlarmBlock extends AlarmBlock {
    public BuzzAlarmBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public SoundEvent getAlarmSound() {
        return AllSoundEvents.ALARM_BUZZ.get();
    }

    @Override
    public int getSoundInterval() {
        return 40;
    }
}
