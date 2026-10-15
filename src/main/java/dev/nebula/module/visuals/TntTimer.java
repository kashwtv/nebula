package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Countdown above lit TNT so you know when to run. */
public final class TntTimer extends Module {
    public TntTimer() {
        super("TNT Timer", "Shows time left on lit TNT.", Category.VISUALS);
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        if (!isEnabled() || mc().level == null || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var cam = e.getCamera().getPosition();
        float pt = e.getPartialTick();
        for (Entity en : mc().level.entitiesForRendering()) {
            if (!(en instanceof PrimedTnt t)) continue;
            float secs = Math.max(0, t.getFuse() - pt) / 20f;
            int c = secs > 2 ? 0xFF55FF55 : secs > 1 ? 0xFFFFFF55 : 0xFFFF5555;
            World3DLabel.draw(e, Mth.lerp(pt, t.xOld, t.getX()) - cam.x, Mth.lerp(pt, t.yOld, t.getY()) - cam.y + 1.4, Mth.lerp(pt, t.zOld, t.getZ()) - cam.z, String.format("%.1fs", secs), c);
        }
    }

    private static final class World3DLabel {
        static void draw(RenderLevelStageEvent e, double x, double y, double z, String s, int c) {
            dev.nebula.render.World3D.label(e.getPoseStack(), e.getCamera(), x, y, z, s, c, 1.2f);
        }
    }
}
