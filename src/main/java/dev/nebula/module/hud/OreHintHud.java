package dev.nebula.module.hud;

/** Which ore is most common at your current Y level (1.18+ generation). */
public final class OreHintHud extends StatHud {
    public OreHintHud() {
        super("Ore Hint", "Best ore to find at your Y level.", 1.0f, 0.9f);
    }

    @Override
    protected String label() {
        return "Best Ore";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player;
        if (p == null || mc().level == null) return "Diamond";
        if (mc().level.dimension() == net.minecraft.world.level.Level.NETHER)
            return p.getY() >= 8 && p.getY() <= 22 ? "Ancient Debris" : p.getY() > 10 && p.getY() < 117 ? "Quartz / Gold" : "-";
        int y = p.getBlockY();
        if (y <= -50) return "Diamond / Redstone";
        if (y <= -16) return "Diamond / Lapis";
        if (y <= 0) return "Lapis / Gold";
        if (y <= 32) return "Iron / Copper";
        if (y <= 96) return "Copper / Coal";
        if (y >= 232) return "Iron (mountains)";
        return "Coal";
    }
}
