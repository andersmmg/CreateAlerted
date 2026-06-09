package com.andersmmg.create_alerted;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class AllSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, Create_alerted.MODID);

    public static final Supplier<SoundEvent> ALARM_BASIC = SOUND_EVENTS.register("alarm_basic",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Create_alerted.MODID, "alarm_basic")));

    public static final Supplier<SoundEvent> ALARM_ANNOYING = SOUND_EVENTS.register("alarm_annoying",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Create_alerted.MODID, "alarm_annoying")));

    public static final Supplier<SoundEvent> ALARM_BUZZ = SOUND_EVENTS.register("alarm_buzz",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Create_alerted.MODID, "alarm_buzz")));
}
