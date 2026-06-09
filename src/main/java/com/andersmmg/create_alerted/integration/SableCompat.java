package com.andersmmg.create_alerted.integration;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SableCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean active = false;
    private static Object helperInstance;
    private static Method projectOutOfSubLevel;

    public static void init() {
        if (ModList.get().isLoaded("sable")) {
            try {
                Class<?> sableClass = Class.forName("dev.ryanhcode.sable.Sable");
                Field helperField = sableClass.getField("HELPER");
                helperInstance = helperField.get(null);

                Class<?> companionClass = Class.forName("dev.ryanhcode.sable.companion.SableCompanion");
                projectOutOfSubLevel = companionClass.getMethod("projectOutOfSubLevel", Level.class, Position.class);

                active = true;
                LOGGER.info("Sable sub-level support enabled.");
            } catch (Exception e) {
                LOGGER.warn("Failed to hook into Sable.", e);
            }
        }
    }

    public static Vec3 getGlobalPos(Level level, Vec3 pos) {
        if (!active || helperInstance == null)
            return pos;
        try {
            return (Vec3) projectOutOfSubLevel.invoke(helperInstance, level, (Position) pos);
        } catch (Exception e) {
            return pos;
        }
    }

    public static boolean isInsideSubLevel(Level level, BlockPos pos) {
        return Math.abs(pos.getX()) > 1000000 || Math.abs(pos.getZ()) > 1000000;
    }
}
