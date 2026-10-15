package dev.nebula.gui;

import dev.nebula.module.world.StructureFinder;
import dev.nebula.structure.StructureSearch;
import dev.nebula.structure.StructureTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Panel: pick a dimension, search, read nearest structures, click a row to pin its beam. */
public final class StructureFinderScreen extends Screen {
    private ResourceKey<Level> dim;
    private double scroll;
    private EditBox seedBox;
    private final List<Object[]> rows = new ArrayList<>();
    private static final int ROW = 11;

    public StructureFinderScreen() {
        super(Component.literal("Structure Finder"));
        var p = net.minecraft.client.Minecraft.getInstance().player;
        dim = p != null ? p.level().dimension() : Level.OVERWORLD;
    }

    @Override
    protected void init() {
        int x = width / 2 - 160;
        addRenderableWidget(Button.builder(Component.literal("Overworld"), b -> { dim = Level.OVERWORLD; scroll = 0; }).bounds(x, 22, 70, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Nether"), b -> { dim = Level.NETHER; scroll = 0; }).bounds(x + 74, 22, 60, 18).build());
        addRenderableWidget(Button.builder(Component.literal("End"), b -> { dim = Level.END; scroll = 0; }).bounds(x + 138, 22, 44, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Search"), b -> StructureFinder.get().search(dim, null, null)).bounds(x + 186, 22, 60, 18).build());
        boolean sp = minecraft.hasSingleplayerServer();
        seedBox = new EditBox(font, x + 250, 22, 70, 18, Component.literal("seed"));
        seedBox.setHint(Component.literal(sp ? "SP: auto" : "seed"));
        if (StructureFinder.seed != null) seedBox.setValue(String.valueOf(StructureFinder.seed));
        seedBox.setEditable(!sp);
        seedBox.setResponder(s -> { if (!s.isBlank()) StructureFinder.seed = dev.nebula.structure.SeedWorld.parseSeed(s); });
        addRenderableWidget(seedBox);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        var sf = StructureFinder.get();
        g.drawCenteredString(font, "Structure Finder  -  " + dim.location().getPath() + "  -  " + sf.status, width / 2, 8, 0xFFE0D0FF);
        rows.clear();
        var res = sf.results.get(dim);
        int y = 48 - (int) scroll;
        int x = width / 2 - 160;
        if (res == null) g.drawString(font, "No results yet for this dimension. Press Search.", x, 48, 0xFFAAAAAA);
        else for (var t : StructureTypes.forDim(dim)) {
            var hits = res.getOrDefault(t.id(), List.of());
            if (y > 40 && y < height - 10) g.drawString(font, t.name() + (hits.isEmpty() ? "  (none in range)" : ""), x, y, t.color());
            y += ROW;
            for (var h : hits) {
                boolean pin = sf.pinned.contains(StructureFinder.pinKey(dim, h));
                String s = (pin ? "\u2605 " : "   ") + h.x() + ", " + h.z() + "   " + (int) h.dist() + "m" + (h.approx() ? "  approx" : "")
                        + (dim == Level.NETHER ? "   (overworld " + h.x() * 8 + ", " + h.z() * 8 + ")" : "");
                if (y > 40 && y < height - 10) {
                    boolean hov = mx >= x && mx < x + 320 && my >= y - 1 && my < y + ROW - 1;
                    g.drawString(font, s, x + 8, y, hov ? 0xFFFFFFFF : 0xFFC8C8C8);
                    rows.add(new Object[]{y, h});
                }
                y += ROW;
            }
        }
        g.drawString(font, "Click a row to pin its beam. Chat: .find <type> [n], .find list, .seed <seed>", x, height - 12, 0xFF888888);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (super.mouseClicked(mx, my, b)) return true;
        for (Object[] r : rows) {
            int y = (int) r[0];
            if (my >= y - 1 && my < y + ROW - 1) {
                var key = StructureFinder.pinKey(dim, (StructureSearch.Hit) r[1]);
                var pins = StructureFinder.get().pinned;
                if (!pins.remove(key)) pins.add(key);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) {
        scroll = Math.max(0, scroll - d * 22);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
