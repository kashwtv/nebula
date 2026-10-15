package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

/** Vanilla glowing outlines (seen through walls) on the entity types you pick. Client-side only. */
public final class Glow extends Module {
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting hostiles = add(new BoolSetting("Hostiles", true));
    public final BoolSetting passives = add(new BoolSetting("Passives", false));
    public final BoolSetting items = add(new BoolSetting("Items", false));
    public final NumberSetting range = add(new NumberSetting("Range", 16, 256, 64, 8, "m"));

    private final Set<Entity> glowing = new HashSet<>();

    public Glow() {
        super("Glow", "Glowing outlines through walls.", Category.VISUALS);
    }

    private boolean wanted(Entity e) {
        if (e instanceof Player pl) return players.get() && !pl.isSpectator();
        if (e instanceof ItemEntity) return items.get();
        if (e instanceof Enemy) return hostiles.get();
        return e instanceof Mob && passives.get();
    }

    @Override
    public void onTick() {
        var p = mc().player;
        Set<Entity> now = new HashSet<>();
        for (Entity e : mc().level.entitiesForRendering())
            if (e != p && e.isAlive() && e.distanceTo(p) <= range.value && wanted(e)) { e.setGlowingTag(true); now.add(e); }
        for (Entity e : glowing) if (!now.contains(e)) e.setGlowingTag(false);
        glowing.clear();
        glowing.addAll(now);
    }

    @Override
    public void onDisable() {
        for (Entity e : glowing) e.setGlowingTag(false);
        glowing.clear();
    }
}
