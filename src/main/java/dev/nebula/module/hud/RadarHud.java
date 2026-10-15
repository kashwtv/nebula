package dev.nebula.module.hud;

import dev.nebula.module.HudModule;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.R2D;
import dev.nebula.render.Skin;
import dev.nebula.render.Theme;
import dev.nebula.util.Friends;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** Top-down radar of players and mobs around you; up is the way you're facing. */
public final class RadarHud extends HudModule {
    public final NumberSetting size = add(new NumberSetting("Size", 60, 160, 90, 2));
    public final NumberSetting range = add(new NumberSetting("Range", 16, 128, 48, 4, "m"));
    public final BoolSetting mobs = add(new BoolSetting("Mobs", true));

    public RadarHud() {
        super("Radar", "Top-down radar of nearby players and mobs.", 1.0f, 0.3f);
    }

    @Override
    public void draw(GuiGraphics g, boolean editor) {
        w = h = size.f();
        Skin.plate(g, 0, 0, w, h);
        float c = w / 2;
        R2D.rect(g, c - 0.5f, 4, 1, h - 8, 0, Theme.TRACK);
        R2D.rect(g, 4, c - 0.5f, w - 8, 1, 0, Theme.TRACK);
        var p = mc().player;
        if (p == null || mc().level == null) return;
        double yaw = Math.toRadians(p.getYRot());
        double cos = Math.cos(yaw), sin = Math.sin(yaw);
        float scale = (c - 4) / range.f();
        for (Entity e : mc().level.entitiesForRendering()) {
            if (e == p || !e.isAlive()) continue;
            int col;
            if (e instanceof Player) col = Friends.is(e) ? 0xFF55FFFF : 0xFFFFFFFF;
            else if (mobs.get() && e instanceof Enemy) col = 0xFFFF5555;
            else if (mobs.get() && e instanceof Mob) col = 0xFF55FF55;
            else continue;
            double dx = e.getX() - p.getX(), dz = e.getZ() - p.getZ();
            double rx = dx * cos + dz * sin, rz = dz * cos - dx * sin; // rotate so facing = up
            float px = (float) (c - rx * scale), py = (float) (c - rz * scale);
            if (px < 3 || py < 3 || px > w - 3 || py > h - 3) continue;
            R2D.circle(g, px, py, e instanceof Player ? 2f : 1.5f, col);
        }
        R2D.circle(g, c, c, 2f, Theme.accent());
    }
}
