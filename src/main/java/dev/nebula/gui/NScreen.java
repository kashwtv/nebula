package dev.nebula.gui;

import dev.nebula.render.R2D;
import dev.nebula.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Base for Nebula's own panels: same units, cards, pills and chips as the Click GUI. */
abstract class NScreen extends Screen {
    record Btn(float x, float y, float w, float h, Runnable run) {
        boolean in(float mx, float my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    protected final List<Btn> btns = new ArrayList<>();
    protected float s = 1, vw, vh;

    protected NScreen(String title) { super(Component.literal(title)); }

    @Override
    public boolean isPauseScreen() { return false; }

    protected abstract void draw(GuiGraphics g, float mx, float my);

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        btns.clear();
        s = R2D.uiScale();
        vw = width / s;
        vh = height / s;
        R2D.smooth = 0.6f / R2D.ppu();
        g.pose().pushPose();
        g.pose().scale(s, s, 1);
        R2D.rect(g, 0, 0, vw, vh, 0, 0x99000000);
        draw(g, mouseX / s, mouseY / s);
        Notifications.render(g, vw, vh);
        g.pose().popPose();
        super.render(g, mouseX, mouseY, pt);
    }

    @Override
    public void renderBackground(GuiGraphics g) {}

    protected void window(GuiGraphics g, float x, float y, float w, float h) {
        R2D.glow(g, x, y + 4, w, h, 10, 44, 0x88000000);
        R2D.rect(g, x, y, w, h, 10, Theme.BG);
        R2D.outline(g, x, y, w, h, 10, R2D.px(), Theme.BORDER);
    }

    protected void card(GuiGraphics g, float x, float y, float w, float h) {
        R2D.rect(g, x, y, w, h, 8, Theme.SURFACE);
        R2D.outline(g, x, y, w, h, 8, R2D.px(), Theme.BORDER);
    }

    protected void header(GuiGraphics g, String left, String right, float x, float y, float w) {
        R2D.text(g, left, x, y, 0xFFA9A9AF);
        if (right != null) R2D.text(g, right, x + w - R2D.width(right), y, Theme.DIM);
    }

    /** Pill chip; selected = accent (white in Mono) with dark text. Returns width. */
    protected float chip(GuiGraphics g, String t, float x, float y, boolean on, float mx, float my, Runnable r) {
        float w = R2D.width(t) + 10, h = 13;
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
        R2D.rect(g, x, y, w, h, 4, on ? Theme.accent() : hov ? Theme.RAISED : Theme.TRACK);
        R2D.text(g, t, x + 5, y + 2.7f, on ? Theme.onAccent() : hov ? Theme.TEXT : Theme.MUTED);
        if (r != null) btns.add(new Btn(x, y, w, h, r));
        return w;
    }

    /** Rounded button. primary = filled accent. Returns width. */
    protected float pill(GuiGraphics g, String t, float x, float y, float w, boolean primary, float mx, float my, Runnable r) {
        if (w <= 0) w = R2D.width(t) + 16;
        float h = 16;
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
        int bg = primary ? (hov ? 0xFFFFFFFF : Theme.accent()) : hov ? Theme.RAISED : Theme.TRACK;
        R2D.rect(g, x, y, w, h, 8, bg);
        R2D.text(g, t, x + (w - R2D.width(t)) / 2, y + 4f, primary ? Theme.onAccent() : hov ? Theme.TEXT : 0xFFC9C9CE);
        if (r != null) btns.add(new Btn(x, y, w, h, r));
        return w;
    }

    /** Lays chips out wrapped inside [x, x+w]; returns the bottom y. */
    protected float chips(GuiGraphics g, List<String> opts, java.util.function.Predicate<String> on, java.util.function.Consumer<String> toggle,
                          float x, float y, float w, float mx, float my) {
        float cx = x, cy = y;
        for (String o : opts) {
            float cw = R2D.width(o) + 10;
            if (cx > x && cx + cw > x + w) { cx = x; cy += 16; }
            cx += chip(g, o, cx, cy, on.test(o), mx, my, () -> toggle.accept(o)) + 3;
        }
        return cy + 13;
    }

    protected dev.nebula.module.setting.NumberSetting dragging;
    private float dragX, dragW;

    /** Slider row like the Click GUI's: name left, value right, thin track with knob. Height 24. */
    protected void slider(GuiGraphics g, dev.nebula.module.setting.NumberSetting n, float x, float y, float w, float mx, float my) {
        if (dragging == n) n.set(n.min + (n.max - n.min) * Math.max(0, Math.min(1, (mx - x) / w)));
        boolean hov = mx >= x - 4 && mx < x + w + 4 && my >= y && my < y + 24;
        String val = n.display();
        R2D.text(g, n.name, x, y + 2, hov ? Theme.TEXT : 0xFFA9A9AF);
        R2D.text(g, val, x + w - R2D.width(val), y + 2, hov || dragging == n ? Theme.TEXT : Theme.MUTED);
        float f = (float) n.fraction(), ty = y + 15;
        R2D.rect(g, x, ty, w, 2, 1, Theme.TRACK);
        if (f > 0) R2D.rect(g, x, ty, Math.max(2, w * f), 2, 1, Theme.accent());
        R2D.circle(g, x + w * f, ty + 1, dragging == n ? 3.6f : hov ? 3.2f : 2.8f, Theme.accent());
        btns.add(new Btn(x - 4, y + 8, w + 8, 14, () -> { dragging = n; dragX = x; dragW = w; }));
    }

    /** Toggle row like the Click GUI's. Height 17. */
    protected void toggle(GuiGraphics g, String name, boolean v, float x, float y, float w, float mx, float my, Runnable r) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + 17;
        R2D.text(g, name, x, y + 4.5f, hov ? Theme.TEXT : 0xFFA9A9AF);
        float tw = 15, th = 8, tx = x + w - tw, ty = y + 4.5f;
        R2D.rect(g, tx, ty, tw, th, th / 2, v ? Theme.accent() : Theme.TRACK);
        R2D.circle(g, tx + 4 + (v ? 7 : 0), ty + th / 2, 2.8f, v ? Theme.onAccent() : 0xFF8B8B91);
        btns.add(new Btn(x, y, w, 17, r));
    }

    @Override
    public boolean mouseReleased(double mx, double my, int b) {
        if (dragging != null) { dragging = null; dev.nebula.config.ConfigIO.save(); }
        return super.mouseReleased(mx, my, b);
    }

    protected void click() {
        minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, 0.2f));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int b) {
        float mx = (float) mouseX / s, my = (float) mouseY / s;
        for (int i = btns.size() - 1; i >= 0; i--) {
            Btn bt = btns.get(i);
            if (bt.in(mx, my)) { click(); bt.run().run(); return true; }
        }
        return clickedAt(mx, my, b) || super.mouseClicked(mouseX, mouseY, b);
    }

    protected boolean clickedAt(float mx, float my, int b) { return false; }
}
