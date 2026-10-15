package dev.nebula.module;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A movable, resizable on-screen element. Position is stored as a fraction of the free
 * screen space (0 = left/top edge, 1 = right/bottom edge) so layouts survive resizes.
 */
public abstract class HudModule extends Module {
    public float fx, fy;
    public float scale = 1f;
    /** Unscaled size measured on the last draw. */
    public float w = 40, h = 14;
    public final dev.nebula.module.setting.BoolSetting background = add(new dev.nebula.module.setting.BoolSetting("Background", true));

    protected HudModule(String name, String description, float fx, float fy) {
        super(name, description, Category.HUD);
        this.fx = fx;
        this.fy = fy;
    }

    @Override
    public boolean listed() {
        return false;
    }

    /** Draw at local origin (0,0). Must set {@link #w} and {@link #h}. */
    public abstract void draw(GuiGraphics g, boolean editor);

    public float x(float vw) {
        return fx * Math.max(0, vw - w * scale);
    }

    public float y(float vh) {
        return fy * Math.max(0, vh - h * scale);
    }

    /** Elements with a stretchable length expose it here; the editor gives them a side handle. */
    public dev.nebula.module.setting.NumberSetting lengthSetting() {
        return null;
    }

    /** True when anchored to the right half, so content should right-align. */
    public boolean rightSide() {
        return fx > 0.5f;
    }

    /** The setting the editor's BG button flips. */
    public dev.nebula.module.setting.BoolSetting backgroundSetting() {
        return background;
    }

    public void render(GuiGraphics g, float vw, float vh, boolean editor) {
        dev.nebula.render.Skin.platesOn = background.get();
        g.pose().pushPose();
        g.pose().translate(x(vw), y(vh), 0);
        g.pose().scale(scale, scale, 1);
        draw(g, editor);
        g.pose().popPose();
        dev.nebula.render.Skin.platesOn = true;
    }

    public void setPos(float x, float y, float vw, float vh) {
        float fw = vw - w * scale, fh = vh - h * scale;
        fx = fw <= 0 ? 0 : Math.max(0, Math.min(1, x / fw));
        fy = fh <= 0 ? 0 : Math.max(0, Math.min(1, y / fh));
    }
}
