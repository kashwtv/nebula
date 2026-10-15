package dev.nebula.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/** Two-pass gaussian blur of whatever is already on screen. */
public final class Blur {
    private static TextureTarget temp;

    private Blur() {}

    /** @param strength blur radius at 1080p; scaled to the real resolution. */
    public static void apply(GuiGraphics g, float strength) {
        ShaderInstance s = Shaders.BLUR;
        if (s == null || strength < 0.3f) return;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        int w = main.width, h = main.height;
        if (temp == null) {
            temp = new TextureTarget(w, h, false, Minecraft.ON_OSX);
            temp.setFilterMode(GL11.GL_LINEAR);
        } else if (temp.width != w || temp.height != h) {
            temp.resize(w, h, Minecraft.ON_OSX);
            temp.setFilterMode(GL11.GL_LINEAR);
        }
        float radius = Math.min(strength * (h / 1080f), 40f);

        g.flush();
        float sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        RenderSystem.disableDepthTest();

        temp.bindWrite(true);
        pass(g, s, main.getColorTextureId(), 1f / w, 0f, radius, sw, sh);
        main.bindWrite(true);
        pass(g, s, temp.getColorTextureId(), 0f, 1f / h, radius, sw, sh);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
    }

    private static void pass(GuiGraphics g, ShaderInstance s, int tex, float dx, float dy, float radius, float sw, float sh) {
        RenderSystem.setShader(() -> s);
        RenderSystem.setShaderTexture(0, tex);
        s.safeGetUniform("Direction").set(dx, dy);
        s.safeGetUniform("Radius").set(radius);
        Matrix4f m = new Matrix4f();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.vertex(m, 0, 0, 0).uv(0, 1).endVertex();
        b.vertex(m, 0, sh, 0).uv(0, 0).endVertex();
        b.vertex(m, sw, sh, 0).uv(1, 0).endVertex();
        b.vertex(m, sw, 0, 0).uv(1, 1).endVertex();
        BufferUploader.drawWithShader(b.end());
    }
}
