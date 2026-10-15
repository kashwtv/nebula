package dev.nebula.module.hud;

/** Name of the block or mob under your crosshair. */
public final class LookingAtHud extends StatHud {
    public LookingAtHud() {
        super("Looking At", "Name of the block or mob under your crosshair.", 0.0f, 0.68f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Target";
    }

    @Override
    protected String value(boolean editor) {
        var hr = mc().hitResult; if (hr instanceof net.minecraft.world.phys.EntityHitResult e) return e.getEntity().getDisplayName().getString(); if (hr instanceof net.minecraft.world.phys.BlockHitResult b && hr.getType() != net.minecraft.world.phys.HitResult.Type.MISS && mc().level != null) return mc().level.getBlockState(b.getBlockPos()).getBlock().getName().getString(); return editor ? "Stone" : "-";
    }
}
