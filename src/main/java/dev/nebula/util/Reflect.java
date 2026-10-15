package dev.nebula.util;

import dev.nebula.Nebula;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Cached access to private Minecraft fields by their SRG names. */
public final class Reflect {
    private static final Map<String, Field> FIELDS = new HashMap<>();
    private static final Map<String, Method> METHODS = new HashMap<>();

    private Reflect() {}

    private static Field field(Class<?> c, String srg) {
        return FIELDS.computeIfAbsent(c.getName() + srg, k -> ObfuscationReflectionHelper.findField(c, srg));
    }

    public static Object get(Class<?> c, Object o, String srg) {
        try {
            return field(c, srg).get(o);
        } catch (Throwable t) {
            Nebula.LOG.warn("reflect get {} failed", srg, t);
            return null;
        }
    }

    public static void set(Class<?> c, Object o, String srg, Object v) {
        try {
            field(c, srg).set(o, v);
        } catch (Throwable t) {
            Nebula.LOG.warn("reflect set {} failed", srg, t);
        }
    }

    public static void setInt(Class<?> c, Object o, String srg, int v) {
        try {
            field(c, srg).setInt(o, v);
        } catch (Throwable t) {
            Nebula.LOG.warn("reflect setInt {} failed", srg, t);
        }
    }

    public static float getFloat(Class<?> c, Object o, String srg) {
        try {
            return field(c, srg).getFloat(o);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static void call(Class<?> c, Object o, String srg, Class<?>[] types, Object... args) {
        try {
            Method m = METHODS.computeIfAbsent(c.getName() + srg, k -> ObfuscationReflectionHelper.findMethod(c, srg, types));
            m.invoke(o, args);
        } catch (Throwable t) {
            Nebula.LOG.warn("reflect call {} failed", srg, t);
        }
    }
}
