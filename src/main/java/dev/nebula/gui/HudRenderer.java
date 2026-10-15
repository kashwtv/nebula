package dev.nebula.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.nebula.module.HudModule;
import dev.nebula.module.ModuleManager;
import dev.nebula.module.visuals.Crosshair;
import dev.nebula.module.visuals.Esp;
import dev.nebula.module.visuals.HitMarker;
import dev.nebula.module.visuals.LowHpWarning;
import dev.nebula.render.R2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** The always-on overlay: world-anchored ESP, crosshair effects, then every HUD element. */
public final class HudRenderer {
    private HudRenderer() {}

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        RenderSystem.enableBlend();
        R2D.alpha = 1;

        // vanilla GUI coordinates
        R2D.smooth = 0.6f / (float) mc.getWindow().getGuiScale();
        ModuleManager.get(LowHpWarning.class).draw(g, width, height);
        ModuleManager.get(Esp.class).drawOverlay(g, width, height);
        Crosshair ch = ModuleManager.get(Crosshair.class);
        if (ch.isEnabled()) ch.draw(g, width, height);
        HitMarker hm = ModuleManager.get(HitMarker.class);
        if (hm.isEnabled()) hm.draw(g, width, height);

        // Nebula UI units
        boolean editor = mc.screen instanceof HudEditor;
        float s = R2D.uiScale();
        float vw = width / s, vh = height / s;
        g.pose().pushPose();
        g.pose().scale(s, s, 1);
        R2D.smooth = 0.6f / R2D.ppu();
        if (!editor && !mc.options.renderDebug) {
            for (HudModule h : ModuleManager.huds()) {
                if (!h.isEnabled()) continue;
                h.render(g, vw, vh, false);
            }
        }
        if (!(mc.screen instanceof ClickGui) && !editor) Notifications.render(g, vw, vh);
        g.pose().popPose();
        R2D.alpha = 1;
    }
}
