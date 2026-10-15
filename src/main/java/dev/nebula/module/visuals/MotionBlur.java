package dev.nebula.module.visuals;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.Shaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Real frame blending: each frame, the previous (already blended) frame is laid over the
 * new one at the chosen strength, then the result is saved for the next frame.
 * Runs before the HUD so your HUD stays sharp.
 */
public final class MotionBlur extends Module {
    public final NumberSetting strength = add(new NumberSetting("Strength", 0.1, 0.9, 0.5, 0.05));

    private TextureTarget history;
    private boolean primed;

    public MotionBlur() {
        super("Motion Blur", "Smooth frame blending when you move the camera.", Category.VISUALS);
    }

    @Override
    public void onEnable() {
        primed = false;
    }

    @SubscribeEvent
    public void onGui(RenderGuiEvent.Pre e) {
        ShaderInstance fade = Shaders.FADE;
        if (!isEnabled() || mc().level == null || fade == null) return;
        Minecraft mc = mc();
        RenderTarget main = mc.getMainRenderTarget();
        int w = main.width, h = main.height;
        if (history == null || history.width != w || history.height != h) {
            if (history == null) history = new TextureTarget(w, h, false, Minecraft.ON_OSX);
            else history.resize(w, h, Minecraft.ON_OSX);
            history.setFilterMode(GL11.GL_NEAREST);
            primed = false;
            main.bindWrite(true);
        }
        e.getGuiGraphics().flush();

        if (primed) {
            float sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(() -> fade);
            RenderSystem.setShaderTexture(0, history.getColorTextureId());
            fade.safeGetUniform("Alpha").set(strength.f());
            Matrix4f m = new Matrix4f();
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            b.vertex(m, 0, 0, 0).uv(0, 1).endVertex();
            b.vertex(m, 0, sh, 0).uv(0, 0).endVertex();
            b.vertex(m, sw, sh, 0).uv(1, 0).endVertex();
            b.vertex(m, sw, 0, 0).uv(1, 1).endVertex();
            BufferUploader.drawWithShader(b.end());
            RenderSystem.enableDepthTest();
        }

        // Save this (blended) frame for next time with a straight framebuffer copy.
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, history.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        main.bindWrite(true);
        primed = true;
    }
}
