package com.andersmmg.create_alerted.block;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AlarmTypeManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final AlarmTypeManager INSTANCE = new AlarmTypeManager();
    private static final Logger LOGGER = LoggerFactory.getLogger("CreateAlerted/AlarmTypes");
    private static final ResourceLocation DEFAULT_ID = ResourceLocation.fromNamespaceAndPath("create_alerted", "basic");
    private static final Map<ResourceLocation, SoundEvent> DYNAMIC_SOUNDS = new ConcurrentHashMap<>();
    private Map<ResourceLocation, AlarmType> types = Map.of();
    private List<ResourceLocation> order = List.of();

    public AlarmTypeManager() {
        super(GSON, "alerted_alarms");
    }

    public static @Nullable SoundEvent getSound(ResourceLocation id) {
        var type = INSTANCE.types.get(id);
        if (type == null) {
            return resolveSound(DEFAULT_ID);
        }
        if (type.soundId() == null) {
            return null;
        }
        return resolveSound(type.soundId());
    }

    private static SoundEvent resolveSound(ResourceLocation soundId) {
        SoundEvent registered = BuiltInRegistries.SOUND_EVENT.get(soundId);
        if (registered != null) {
            return registered;
        }
        return DYNAMIC_SOUNDS.computeIfAbsent(soundId, key -> {
            LOGGER.warn("Sound event for {} not registered; using a dynamically created event (declared only in sounds.json?)", key);
            return SoundEvent.createVariableRangeEvent(key);
        });
    }

    public static @Nullable Integer getInterval(ResourceLocation id) {
        var type = INSTANCE.types.get(id);
        if (type == null) return 30;
        return type.interval();
    }

    public static String translationKey(ResourceLocation id) {
        String path = id.getPath();
        String name = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        return "alarm_type." + name;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, AlarmType> newTypes = new LinkedHashMap<>();
        for (var entry : map.entrySet()) {
            AlarmType.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(error -> LOGGER.error("Failed to parse alarm type {}: {}", entry.getKey(), error))
                    .ifPresent(type -> newTypes.put(entry.getKey(), type));
        }
        this.types = Map.copyOf(newTypes);
        this.order = List.copyOf(new ArrayList<>(newTypes.keySet()));
        LOGGER.info("Loaded {} alarm types", newTypes.size());
    }

    public AlarmType getType(ResourceLocation id) {
        return types.get(id);
    }

    public List<ResourceLocation> getOrder() {
        return order;
    }

    public ResourceLocation getDefaultId() {
        return types.containsKey(DEFAULT_ID) ? DEFAULT_ID : order.isEmpty() ? DEFAULT_ID : order.getFirst();
    }
}
