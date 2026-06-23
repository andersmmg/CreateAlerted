package com.andersmmg.create_alerted;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class AllSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, CreateAlerted.MODID);

    public static final Supplier<SoundEvent> SMOKE_DETECTOR = SOUND_EVENTS.register("smoke_detector",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreateAlerted.MODID, "smoke_detector")));
}
