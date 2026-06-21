package com.andersmmg.create_alerted;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = CreateAlerted.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue ALARM_VOLUME = BUILDER.comment("Volume of the alarm sound (0.0 - 1.0)").defineInRange("alarmVolume", 1.0, 0.0, 1.0);

    private static final ModConfigSpec.IntValue SMOKE_DETECTOR_RADIUS = BUILDER.comment("Detection radius (in blocks) of the smoke detector. Fire within this radius will trigger the alarm and emit a redstone signal.").defineInRange("smokeDetectorRadius", 5, 1, 32);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static double alarmVolume;
    public static int smokeDetectorRadius;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        alarmVolume = ALARM_VOLUME.get();
        smokeDetectorRadius = SMOKE_DETECTOR_RADIUS.get();
    }
}
