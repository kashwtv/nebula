package dev.nebula.module.hud;

import dev.nebula.module.HudModule;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.render.R2D;
import dev.nebula.render.Skin;
import dev.nebula.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** XYZ, the other dimension's coords, facing and biome. */
public final class Coordinates extends HudModule {
    public final BoolSetting decimals = add(new BoolSetting("Decimals", false));
    public final BoolSetting otherDim = add(new BoolSetting("Nether / Overworld", true));
    public final BoolSetting facing = add(new BoolSetting("Facing", true));
    public final BoolSetting biome = add(new BoolSetting("Biome", false));
    public final BoolSetting chunk = add(new BoolSetting("Chunk", false));

    public Coordinates() {
        super("Coordinates", "Where you are and which way you're facing.", 0.0f, 0.06f);
        setEnabled(true);
    }

    private String fmt(double v) {
        return decimals.get() ? String.format("%.1f", v) : String.valueOf((long) Math.floor(v));
    }

    @Override
    public void draw(GuiGraphics g, boolean editor) {
        var p = mc().player;
        if (p == null) return;
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"XYZ", fmt(p.getX()) + "  " + fmt(p.getY()) + "  " + fmt(p.getZ())});
        if (otherDim.get() && mc().level != null && mc().level.dimension() != Level.END) {
            boolean nether = mc().level.dimension() == Level.NETHER;
            double k = nether ? 8 : 0.125;
            rows.add(new String[]{nether ? "Overworld" : "Nether", fmt(p.getX() * k) + "  " + fmt(p.getZ() * k)});
        }
        if (facing.get()) {
            var d = p.getDirection();
            String axis = switch (d) {
                case NORTH -> "-Z";
                case SOUTH -> "+Z";
                case EAST -> "+X";
                default -> "-X";
            };
            String name = d.getName().substring(0, 1).toUpperCase() + d.getName().substring(1);
            rows.add(new String[]{"Facing", name + "  " + axis});
        }
        if (biome.get() && mc().level != null) {
            String b = mc().level.getBiome(p.blockPosition()).unwrapKey().map(k -> pretty(k.location().getPath())).orElse("?");
            rows.add(new String[]{"Biome", b});
        }
        if (chunk.get()) rows.add(new String[]{"Chunk", (p.getBlockX() >> 4) + "  " + (p.getBlockZ() >> 4)});

        float lw = 0, vw = 0;
        for (String[] r : rows) {
            lw = Math.max(lw, R2D.width(r[0]));
            vw = Math.max(vw, R2D.width(r[1]));
        }
        w = 6 + lw + 8 + vw + 6;
        h = 4 + rows.size() * 10 + 1;
        Skin.plate(g, 0, 0, w, h);
        float y = 4;
        for (String[] r : rows) {
            R2D.text(g, r[0], 6, y, Theme.MUTED);
            R2D.text(g, r[1], 6 + lw + 8, y, Theme.TEXT);
            y += 10;
        }
    }

    private static String pretty(String id) {
        StringBuilder sb = new StringBuilder();
        for (String part : id.split("_")) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
