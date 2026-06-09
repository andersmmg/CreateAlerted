package com.andersmmg.create_alerted;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Create_alerted.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue ALARM_VOLUME = BUILDER.comment("Volume of the alarm sound (0.0 - 1.0)").defineInRange("alarmVolume", 1.0, 0.0, 1.0);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static double alarmVolume;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        alarmVolume = ALARM_VOLUME.get();
    }
}
