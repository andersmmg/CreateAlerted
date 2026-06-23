package com.andersmmg.create_alerted.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record AlarmType(@Nullable ResourceLocation soundId, @Nullable Integer interval) {
    public static final Codec<AlarmType> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.optionalFieldOf("sound").forGetter(type -> Optional.ofNullable(type.soundId())),
                    Codec.INT.optionalFieldOf("interval").forGetter(type -> Optional.ofNullable(type.interval()))
            ).apply(instance, (sound, interval) -> new AlarmType(sound.orElse(null), interval.orElse(null)))
    );
}
