package dev.nebula.module.hud;

import dev.nebula.module.Category;
import dev.nebula.module.HudModule;
import dev.nebula.module.Module;
import dev.nebula.module.ModuleManager;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.R2D;
import dev.nebula.render.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** List of active modules; entries slide and collapse smoothly as you toggle. */
public final class ModuleList extends HudModule {
    public final ModeSetting sort = add(new ModeSetting("Sort", "Length", "Length", "Alphabetical"));
    public final ModeSetting textColor = add(new ModeSetting("Text Color", "White", "White", "Accent"));
    public final BoolSetting suffixes = add(new BoolSetting("Suffixes", true));
    public final BoolSetting rowBackground = add(new BoolSetting("Row Background", true));
    public final BoolSetting bar = add(new BoolSetting("Accent Bar", true));
    public final NumberSetting rowHeight = add(new NumberSetting("Row Height", 9, 15, 11, 1));
    public final NumberSetting bgOpacity = add(new NumberSetting("Background Opacity", 0, 1, 0.55, 0.05));

    public ModuleList() {
        super("Module List", "Smoothly animated list of what you have on.", 1.0f, 0.0f);
        settings.remove(background);
        setEnabled(true);
    }

    @Override
    public BoolSetting backgroundSetting() {
        return rowBackground;
    }

    private String text(Module m) {
        String s = suffixes.get() ? m.suffix() : null;
        return s == null ? m.name : m.name + " " + s;
    }

    @Override
    public void draw(GuiGraphics g, boolean editor) {
        List<Module> mods = new ArrayList<>();
        for (Module m : ModuleManager.MODULES) {
            boolean on = m.toggleable() && m.listed() && (m.isEnabled() || (editor && m.category == Category.VISUALS && mods.size() < 4));
            m.listAnim.update(on ? 1 : 0, 10);
            if (m.listAnim.get() > 0.01f) mods.add(m);
        }
        if (sort.is("Length")) mods.sort(Comparator.comparingDouble((Module m) -> -R2D.width(text(m))));
        else mods.sort(Comparator.comparing(m -> m.name));

        float maxW = 30;
        for (Module m : mods) maxW = Math.max(maxW, R2D.width(text(m)) + 9);
        boolean right = rightSide();
        float rh = rowHeight.f();
        float y = 0;
        int acc = Theme.accent();
        int bg = ((int) (bgOpacity.f() * 255) << 24) | 0x0C0C0E;
        for (Module m : mods) {
            float a = m.listAnim.get();
            String name = m.name;
            String suf = suffixes.get() ? m.suffix() : null;
            float bw = R2D.width(text(m)) + 9;
            float slide = (1 - a) * 18;
            float bx = right ? maxW - bw + slide : -slide;
            float prev = R2D.alpha;
            R2D.alpha = prev * a;
            if (rowBackground.get()) R2D.rect(g, bx, y, bw, rh, 0, bg);
            if (bar.get()) R2D.rect(g, right ? bx + bw - 1.5f : bx, y, 1.5f, rh, 0, acc);
            float tx = right ? bx + 3.5f : bx + 5;
            float ty = y + (rh - 8) / 2f;
            R2D.text(g, name, tx, ty, textColor.is("Accent") ? acc : Theme.TEXT);
            if (suf != null) R2D.text(g, suf, tx + R2D.width(name + " "), ty, Theme.MUTED);
            R2D.alpha = prev;
            y += rh * a;
        }
        w = maxW;
        h = Math.max(rh, y);
    }
}
