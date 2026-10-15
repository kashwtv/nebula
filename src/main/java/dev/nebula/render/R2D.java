package dev.nebula.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.util.Col;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Shader-backed 2D drawing: rounded rects, gradients, outlines, glows, text.
 * Everything is multiplied by {@link #alpha} so whole groups can fade together.
 */
public final class R2D {
    /** Global alpha multiplier applied to every draw. */
    public static float alpha = 1f;
    /** Anti-alias width in current units (about one physical pixel). */
    public static float smooth = 0.5f;

    private static final Deque<int[]> SCISSORS = new ArrayDeque<>();

    private R2D() {}

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    /** Physical pixels per Nebula UI unit; integer so the pixel font stays crisp. */
    /**
     * Physical pixels per Nebula UI unit, in half steps (1, 1.5, 2, 2.5 ...) so the UI is
     * always drawn at the screen's real resolution. Text is rasterized to match exactly.
     */
    public static float ppu() {
        var win = mc().getWindow();
        float auto = Math.max(1f, Math.round(win.getHeight() / 560f * 2) / 2f);
        dev.nebula.module.client.ClickGuiModule c = dev.nebula.module.ModuleManager.get(dev.nebula.module.client.ClickGuiModule.class);
        float want = c == null || c.uiScale.is("Auto") ? auto : c.uiScale.index;
        float cap = (float) Math.floor(Math.max(1f, win.getHeight() / 240f) * 2) / 2f;
        return Math.max(1f, Math.min(4f, Math.min(want, cap)));
    }

    /** Half-step index (2..8) used to pick the matching font rasterization. */
    private static int fontStep() {
        return Math.max(2, Math.min(8, Math.round(ppu() * 2)));
    }

    /** Pose scale that converts vanilla GUI units into Nebula UI units. */
    public static float uiScale() {
        return (float) (ppu() / mc().getWindow().getGuiScale());
    }

    /** One physical pixel in Nebula UI units (hairlines). */
    public static float px() {
        return 1f / ppu();
    }

    private static int a(int c) {
        return alpha >= 1f ? c : Col.alpha(c, alpha);
    }

    // ---------------------------------------------------------------- shapes

    public static void rect(GuiGraphics g, float x, float y, float w, float h, float r, int color) {
        shape(g, x, y, w, h, r, 0, 0, color, color, color, color);
    }

    public static void hGradient(GuiGraphics g, float x, float y, float w, float h, float r, int left, int right) {
        shape(g, x, y, w, h, r, 0, 0, left, right, right, left);
    }

    public static void vGradient(GuiGraphics g, float x, float y, float w, float h, float r, int top, int bottom) {
        shape(g, x, y, w, h, r, 0, 0, top, top, bottom, bottom);
    }

    public static void gradient(GuiGraphics g, float x, float y, float w, float h, float r, int tl, int tr, int br, int bl) {
        shape(g, x, y, w, h, r, 0, 0, tl, tr, br, bl);
    }

    public static void outline(GuiGraphics g, float x, float y, float w, float h, float r, float thickness, int color) {
        shape(g, x, y, w, h, r, thickness, 0, color, color, color, color);
    }

    public static void outlineGradient(GuiGraphics g, float x, float y, float w, float h, float r, float thickness, int left, int right) {
        shape(g, x, y, w, h, r, thickness, 0, left, right, right, left);
    }

    /** Soft glow / shadow spreading {@code spread} units outside the rect. */
    public static void glow(GuiGraphics g, float x, float y, float w, float h, float r, float spread, int color) {
        shape(g, x, y, w, h, r, 0, spread, color, color, color, color);
    }

    public static void glowGradient(GuiGraphics g, float x, float y, float w, float h, float r, float spread, int left, int right) {
        shape(g, x, y, w, h, r, 0, spread, left, right, right, left);
    }

    public static void circle(GuiGraphics g, float cx, float cy, float radius, int color) {
        rect(g, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    public static void ring(GuiGraphics g, float cx, float cy, float radius, float thickness, int color) {
        outline(g, cx - radius, cy - radius, radius * 2, radius * 2, radius, thickness, color);
    }

    private static void shape(GuiGraphics g, float x, float y, float w, float h, float r, float thickness, float pad,
                              int tl, int tr, int br, int bl) {
        ShaderInstance s = Shaders.ROUNDED;
        if (s == null || w <= 0 || h <= 0) return;
        tl = a(tl); tr = a(tr); br = a(br); bl = a(bl);
        if (((tl | tr | br | bl) >>> 24) == 0) return;
        float px = x - pad, py = y - pad, pw = w + pad * 2, ph = h + pad * 2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> s);
        s.safeGetUniform("Size").set(w, h);
        s.safeGetUniform("Radius").set(Math.max(0f, Math.min(r, Math.min(w, h) / 2f)));
        s.safeGetUniform("Thickness").set(thickness);
        s.safeGetUniform("Pad").set(pad);
        s.safeGetUniform("Smooth").set(smooth);
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        b.vertex(m, px, py, 0).uv(0, 0).color(tl).endVertex();
        b.vertex(m, px, py + ph, 0).uv(0, 1).color(bl).endVertex();
        b.vertex(m, px + pw, py + ph, 0).uv(1, 1).color(br).endVertex();
        b.vertex(m, px + pw, py, 0).uv(1, 0).color(tr).endVertex();
        BufferUploader.drawWithShader(b.end());
    }

    /** Straight line segments in one batch: {x1,y1,x2,y2,...} with one color per segment. */
    public static void lines(GuiGraphics g, float[] pts, int[] colors, int count) {
        if (count == 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(Math.max(1f, ppu() * 0.6f));
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < count; i++) {
            int c = a(colors[i]);
            b.vertex(m, pts[i * 4], pts[i * 4 + 1], 0).color(c).endVertex();
            b.vertex(m, pts[i * 4 + 2], pts[i * 4 + 3], 0).color(c).endVertex();
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.lineWidth(1f);
    }

    // ------------------------------------------------------------------ text

    // ------------------------------------------------------------------ fonts

    private static final net.minecraft.network.chat.Style[] UI = new net.minecraft.network.chat.Style[9];
    private static final net.minecraft.network.chat.Style[] UI_BOLD = new net.minecraft.network.chat.Style[9];
    private static final net.minecraft.network.chat.Style[] UI_TITLE = new net.minecraft.network.chat.Style[9];

    private static net.minecraft.network.chat.Style titleStyle() {
        int p = fontStep();
        if (UI_TITLE[p] == null) UI_TITLE[p] = net.minecraft.network.chat.Style.EMPTY.withFont(new net.minecraft.resources.ResourceLocation("nebula", "ui_title_h" + p));
        return UI_TITLE[p];
    }

    /** Large heading text (falls back to bold when the Minecraft font is selected). */
    public static void title(GuiGraphics g, String s, float x, float y, int color) {
        if (!customFont()) {
            bold(g, s, x, y + 2, color);
            return;
        }
        int c = a(color);
        if (((c >>> 24) & 0xFF) < 6) return;
        g.pose().pushPose();
        g.pose().translate(snap(x), snap(y), 0);
        g.drawString(font(), net.minecraft.network.chat.Component.literal(s).withStyle(titleStyle()), 0, 0, c, false);
        g.pose().popPose();
    }

    public static float titleWidth(String s) {
        if (!customFont()) return boldWidth(s);
        return font().width(net.minecraft.network.chat.Component.literal(s).withStyle(titleStyle()));
    }

    /** Poppins rasterized at exactly the current pixel scale, so it stays sharp. */
    private static net.minecraft.network.chat.Style uiStyle(boolean bold) {
        int p = fontStep();
        var arr = bold ? UI_BOLD : UI;
        if (arr[p] == null) {
            arr[p] = net.minecraft.network.chat.Style.EMPTY.withFont(
                    new net.minecraft.resources.ResourceLocation("nebula", (bold ? "ui_bold_h" : "ui_h") + p));
        }
        return arr[p];
    }

    public static boolean customFont() {
        dev.nebula.module.client.ClickGuiModule c = dev.nebula.module.ModuleManager.get(dev.nebula.module.client.ClickGuiModule.class);
        return c == null || c.font.is("Poppins");
    }

    private static net.minecraft.network.chat.Component comp(String s, boolean bold) {
        if (customFont()) return net.minecraft.network.chat.Component.literal(s).withStyle(uiStyle(bold));
        var c = net.minecraft.network.chat.Component.literal(s);
        return bold ? c.withStyle(net.minecraft.ChatFormatting.BOLD) : c;
    }

    public static float width(String s) {
        return font().width(comp(s, false));
    }

    public static void text(GuiGraphics g, String s, float x, float y, int color) {
        // HUD elements without a background get a drop shadow so they stay readable
        text(g, s, x, y, color, !Skin.platesOn);
    }

    /** Snap to the physical pixel grid so the pixel font never lands between pixels. */
    private static float snap(float v) {
        float p = ppu();
        return Math.round(v * p) / p;
    }

    public static void text(GuiGraphics g, String s, float x, float y, int color, boolean shadow) {
        int c = a(color);
        if (((c >>> 24) & 0xFF) < 6) return;
        g.pose().pushPose();
        g.pose().translate(snap(x), snap(y), 0);
        g.drawString(font(), comp(s, false), 0, 0, c, shadow);
        g.pose().popPose();
    }

    public static void bold(GuiGraphics g, String s, float x, float y, int color) {
        bold(g, s, x, y, color, false);
    }

    public static void bold(GuiGraphics g, String s, float x, float y, int color, boolean shadow) {
        int c = a(color);
        if (((c >>> 24) & 0xFF) < 6) return;
        g.pose().pushPose();
        g.pose().translate(snap(x), snap(y), 0);
        g.drawString(font(), comp(s, true), 0, 0, c, shadow);
        g.pose().popPose();
    }

    public static float boldWidth(String s) {
        return font().width(comp(s, true));
    }

    public static void textCentered(GuiGraphics g, String s, float cx, float y, int color) {
        text(g, s, cx - width(s) / 2f, y, color);
    }

    public static void textRight(GuiGraphics g, String s, float right, float y, int color) {
        text(g, s, right - width(s), y, color);
    }

    /** Animated per-letter gradient text using the current theme accent. */
    public static float accentText(GuiGraphics g, String s, float x, float y, float step) {
        float cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            text(g, ch, cx, y, Theme.accent(i * step));
            cx += width(ch);
        }
        return cx - x;
    }

    public static void scaledText(GuiGraphics g, String s, float x, float y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        text(g, s, 0, 0, color);
        g.pose().popPose();
    }

    // -------------------------------------------------------------- scissors

    /** Clip to a rect given in current pose coordinates; nested clips intersect. */
    public static void pushScissor(GuiGraphics g, float x, float y, float w, float h) {
        g.flush();
        Matrix4f m = g.pose().last().pose();
        Vector4f p1 = m.transform(new Vector4f(x, y, 0, 1));
        Vector4f p2 = m.transform(new Vector4f(x + w, y + h, 0, 1));
        double gs = mc().getWindow().getGuiScale();
        int[] r = {
                (int) Math.floor(Math.min(p1.x, p2.x) * gs), (int) Math.floor(Math.min(p1.y, p2.y) * gs),
                (int) Math.ceil(Math.max(p1.x, p2.x) * gs), (int) Math.ceil(Math.max(p1.y, p2.y) * gs)
        };
        int[] top = SCISSORS.peek();
        if (top != null) {
            r[0] = Math.max(r[0], top[0]);
            r[1] = Math.max(r[1], top[1]);
            r[2] = Math.min(r[2], top[2]);
            r[3] = Math.min(r[3], top[3]);
        }
        SCISSORS.push(r);
        applyScissor(r);
    }

    public static void popScissor(GuiGraphics g) {
        g.flush();
        SCISSORS.poll();
        int[] top = SCISSORS.peek();
        if (top == null) RenderSystem.disableScissor();
        else applyScissor(top);
    }

    private static void applyScissor(int[] r) {
        Window win = mc().getWindow();
        int w = Math.max(0, r[2] - r[0]), h = Math.max(0, r[3] - r[1]);
        RenderSystem.enableScissor(r[0], win.getHeight() - r[3], w, h);
    }
}
