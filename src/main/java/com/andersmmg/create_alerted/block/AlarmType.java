package com.andersmmg.create_alerted.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record AlarmType(ResourceLocation soundId, int interval) {
    public static final Codec<AlarmType> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("sound").forGetter(AlarmType::soundId),
                    Codec.INT.fieldOf("interval").forGetter(AlarmType::interval)
            ).apply(instance, AlarmType::new)
    );
}
