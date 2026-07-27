package com.andersmmg.create_alerted.block;

import com.andersmmg.create_alerted.CreateAlerted;
import net.minecraft.resources.ResourceLocation;

public enum AlarmVisualType {
    SPIN("spin"),
    BLINK("blink"),
    FLASHING("flashing");

    private final ResourceLocation id;

    AlarmVisualType(String path) {
        this.id = ResourceLocation.fromNamespaceAndPath(CreateAlerted.MODID, path);
    }

    public static AlarmVisualType byId(ResourceLocation id) {
        for (AlarmVisualType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return SPIN;
    }

    public ResourceLocation id() {
        return id;
    }
}
