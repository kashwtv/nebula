package dev.nebula.module.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.World3D;
import dev.nebula.util.Friends;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Lines from your crosshair to players and mobs. Colour fades from red (close) to green (far). */
public final class Tracers extends Module {
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting hostiles = add(new BoolSetting("Hostiles", false));
    public final BoolSetting passives = add(new BoolSetting("Passives", false));
    public final BoolSetting distanceColor = add(new BoolSetting("Distance Color", true));
    public final ColorSetting color = add(new ColorSetting("Color", 0.55f));
    public final NumberSetting range = add(new NumberSetting("Range", 16, 256, 96, 8, "m"));
    public final NumberSetting width = add(new NumberSetting("Width", 1, 4, 1.5, 0.5));

    public Tracers() {
        super("Tracers", "Draws lines to nearby players and mobs.", Category.VISUALS);
    }

    private boolean wanted(Entity e) {
        if (e instanceof Player pl) return players.get() && !pl.isSpectator();
        if (e instanceof Enemy) return hostiles.get();
        return e instanceof Mob && passives.get();
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        var p = mc().player;
        if (!isEnabled() || p == null || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var cam = e.getCamera().getPosition();
        var m = e.getPoseStack().last().pose();
        var look = e.getCamera().getLookVector();
        float pt = e.getPartialTick();
        RenderSystem.lineWidth(width.f());
        var b = World3D.begin(VertexFormat.Mode.DEBUG_LINES, false);
        for (Entity en : mc().level.entitiesForRendering()) {
            if (en == p || !en.isAlive() || !wanted(en) || en.distanceTo(p) > range.value) continue;
            float d = (float) Math.min(1, en.distanceTo(p) / 40.0);
            int c = Friends.is(en) ? 0xFF55FFFF : distanceColor.get() ? dev.nebula.util.Col.health(d) : color.get();
            double x = Mth.lerp(pt, en.xOld, en.getX()) - cam.x, y = Mth.lerp(pt, en.yOld, en.getY()) - cam.y + en.getBbHeight() * 0.5, z = Mth.lerp(pt, en.zOld, en.getZ()) - cam.z;
            World3D.v(b, m, look.x() * 0.5, look.y() * 0.5, look.z() * 0.5, c, 0.85f);
            World3D.v(b, m, x, y, z, c, 0.85f);
        }
        World3D.end(b);
    }
}
