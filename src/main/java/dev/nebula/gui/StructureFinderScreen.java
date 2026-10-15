package dev.nebula.gui;

import dev.nebula.config.ConfigIO;
import dev.nebula.module.world.StructureFinder;
import dev.nebula.render.R2D;
import dev.nebula.render.Theme;
import dev.nebula.structure.StructureTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Structure Finder panel in Nebula's card style: chip picker + settings on the left, results on the right. */
public final class StructureFinderScreen extends NScreen {
    private ResourceKey<Level> dim;
    private float scroll, maxScroll;
    private EditBox seedBox;
    private float wx, wy, ww, wh;

    public StructureFinderScreen() {
        super("Structure Finder");
        var p = net.minecraft.client.Minecraft.getInstance().player;
        dim = p != null ? p.level().dimension() : Level.OVERWORLD;
    }

    private void layout() {
        float sc = R2D.uiScale();
        vw = width / sc; vh = height / sc;
        ww = Math.min(vw - 24, 600);
        wh = Math.min(vh - 24, 350);
        wx = (vw - ww) / 2;
        wy = (vh - wh) / 2;
    }

    @Override
    protected void init() {
        layout();
        float sc = R2D.uiScale();
        if (minecraft.hasSingleplayerServer()) return; // seed comes from the world itself
        float lx = wx + 14, lw = 228;
        seedBox = new EditBox(font, (int) ((lx + 40) * sc), (int) ((wy + wh - 26) * sc - 4), (int) ((lw - 46) * sc), 10, Component.literal("seed"));
        seedBox.setBordered(false);
        seedBox.setTextColor(0xEDEDEF);
        seedBox.setHint(Component.literal("paste /seed here").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        if (StructureFinder.seed != null) seedBox.setValue(String.valueOf(StructureFinder.seed));
        seedBox.setResponder(v -> { if (!v.isBlank()) StructureFinder.seed = dev.nebula.structure.SeedWorld.parseSeed(v); });
        addRenderableWidget(seedBox);
    }

    @Override
    protected void draw(GuiGraphics g, float mx, float my) {
        layout();
        var sf = StructureFinder.get();
        window(g, wx, wy, ww, wh);
        R2D.bold(g, "Structure Finder", wx + 14, wy + 12, Theme.TEXT);
        String st = sf.busy ? "Searching..." : sf.status;
        st = SettingsView.fit(st, ww - 150);
        R2D.text(g, st, wx + ww - 14 - R2D.width(st), wy + 13, sf.busy ? Theme.YELLOW : Theme.DIM);

        float lx = wx + 14, ly = wy + 38, lw = 228, lh = wh - 46;
        card(g, lx - 8, ly - 6, lw + 16, lh);
        toggle(g, "Enabled", sf.isEnabled(), lx, ly, lw, mx, my, () -> { sf.toggle(); ConfigIO.save(); });
        ly += 21;
        header(g, "Structures", sf.structures.selected.size() + " selected", lx, ly, lw);
        ly += 13;
        var opts = List.of(sf.structures.options);
        ly = chips(g, opts, sf.structures::has, o -> { sf.structures.toggle(o); ConfigIO.save(); }, lx, ly, lw, mx, my) + 6;
        float bx = lx;
        bx += pill(g, "All", bx, ly, 0, false, mx, my, () -> { sf.structures.selected.addAll(opts); ConfigIO.save(); }) + 4;
        bx += pill(g, "None", bx, ly, 0, false, mx, my, () -> { sf.structures.selected.clear(); ConfigIO.save(); }) + 4;
        pill(g, "This dimension", bx, ly, 0, false, mx, my, this::onlyThisDim);
        ly += 24;
        slider(g, sf.radius, lx, ly, lw, mx, my);
        ly += 26;
        slider(g, sf.count, lx, ly, lw, mx, my);
        R2D.text(g, "Seed", lx, wy + wh - 29.5f, 0xFFA9A9AF);
        float fh = seedBox == null ? 14 : Math.max(14, 9 / R2D.uiScale() + 6);
        R2D.rect(g, lx + 34, wy + wh - 28 - fh / 2 + 2, lw - 34, fh, 5, Theme.TRACK);
        if (seedBox == null) R2D.text(g, "automatic (singleplayer world)", lx + 40, wy + wh - 29.5f, Theme.MUTED);

        float rx = lx + lw + 22, ry = wy + 38, rw = wx + ww - 14 - rx, rh = wh - 46;
        card(g, rx - 8, ry - 6, rw + 16, rh);
        float tx = rx;
        for (var d : List.of(Level.OVERWORLD, Level.NETHER, Level.END)) {
            String n = d == Level.OVERWORLD ? "Overworld" : d == Level.NETHER ? "Nether" : "End";
            tx += chip(g, n, tx, ry, dim == d, mx, my, () -> { dim = d; scroll = 0; }) + 3;
        }
        float sbw = R2D.width("Search Selected") + 16;
        pill(g, "Search Selected", rx + rw - sbw, ry - 1.5f, sbw, true, mx, my, () -> sf.searchSelected(dim));
        if (dim == Level.OVERWORLD)
            pill(g, "Portal Room", rx + rw - sbw - 4 - (R2D.width("Portal Room") + 16), ry - 1.5f, 0, false, mx, my, sf::findPortalRoom);

        float top = ry + 22, bottom = ry + rh - 24;
        R2D.rect(g, rx, top - 4, rw, R2D.px(), 0, 0x14FFFFFF);
        var res = sf.results.get(dim);
        var types = sf.selectedTypes(dim);
        R2D.pushScissor(g, rx - 4, top, rw + 8, bottom - top);
        float y = top + 2 - scroll;
        if (types.isEmpty()) R2D.text(g, "No selected structures generate in this dimension.", rx, y + 2, Theme.DIM);
        else if (res == null) R2D.text(g, "No results yet. Press Search Selected.", rx, y + 2, Theme.DIM);
        else {
            for (var t : types) {
                var hits = res.getOrDefault(t.id(), List.of());
                R2D.circle(g, rx + 3, y + 5, 2.6f, t.color());
                R2D.bold(g, t.name(), rx + 10, y + 1, Theme.TEXT);
                String c = hits.isEmpty() ? "none in range" : hits.size() + (hits.size() == 1 ? " result" : " results");
                R2D.text(g, c, rx + rw - R2D.width(c), y + 1.5f, Theme.DIM);
                y += 14;
                for (var h : hits) {
                    String key = StructureFinder.pinKey(dim, h);
                    boolean pin = sf.pinned.contains(key);
                    boolean hov = mx >= rx - 4 && mx < rx + rw + 4 && my >= y && my < y + 14 && my >= top && my < bottom;
                    if (hov || pin) R2D.rect(g, rx - 4, y, rw + 8, 14, 4, pin ? Theme.RAISED : 0xFF17171A);
                    String coords = h.x() + ", " + h.z() + (dim == Level.NETHER ? "    OW " + h.x() * 8 + ", " + h.z() * 8 : "");
                    R2D.text(g, coords, rx + 10, y + 3.5f, hov ? Theme.TEXT : 0xFFC9C9CE);
                    String dist = (int) h.dist() + "m" + (h.approx() ? " ~" : "");
                    R2D.text(g, dist, rx + rw - R2D.width(dist) - (pin ? 32 : 0), y + 3.5f, Theme.MUTED);
                    if (pin) { R2D.rect(g, rx + rw - 27, y + 2.5f, 27, 9, 4, Theme.accent()); R2D.text(g, "beam", rx + rw - 23.5f, y + 3.3f, Theme.onAccent()); }
                    if (y + 14 > top && y < bottom) {
                        float by = Math.max(y, top);
                        btns.add(new Btn(rx - 4, by, rw + 8, Math.min(y + 14, bottom) - by, () -> { if (!sf.pinned.remove(key)) sf.pinned.add(key); }));
                    }
                    y += 15;
                }
                y += 5;
            }
            if (dim == Level.OVERWORLD && sf.portalInfo != null && sf.structures.has("Portal Room")) {
                R2D.circle(g, rx + 3, y + 5, 2.6f, 0xFF33FF66);
                R2D.text(g, sf.portalInfo, rx + 10, y + 1.5f, 0xFF33FF66);
                y += 16;
            }
        }
        maxScroll = Math.max(0, y + scroll - bottom);
        R2D.popScissor(g);
        R2D.text(g, "Click a result to pin its beam   .find <type>   .seed <seed>", rx, ry + rh - 18, Theme.DIM);
    }

    private void onlyThisDim() {
        var sf = StructureFinder.get();
        sf.structures.selected.clear();
        var ids = new ArrayList<String>();
        for (var t : StructureTypes.forDim(dim)) ids.add(t.id());
        StructureFinder.CHIPS.forEach((label, list) -> { if (list.stream().anyMatch(ids::contains)) sf.structures.selected.add(label); });
        ConfigIO.save();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - (float) d * 22));
        return true;
    }
}
