package dev.nebula.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Players you never want combat modules to touch. Middle-click someone to add them. */
public final class Friends {
    public static final Set<String> NAMES = new LinkedHashSet<>();

    private Friends() {}

    public static boolean is(Entity e) {
        return e instanceof Player p && NAMES.contains(p.getGameProfile().getName().toLowerCase(Locale.ROOT));
    }

    /** @return true if now a friend. */
    public static boolean toggle(Player p) {
        String n = p.getGameProfile().getName().toLowerCase(Locale.ROOT);
        if (!NAMES.remove(n)) {
            NAMES.add(n);
            return true;
        }
        return false;
    }
}
