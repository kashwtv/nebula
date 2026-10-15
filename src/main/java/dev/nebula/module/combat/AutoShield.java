package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Targets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ShieldItem;

/** Raises your offhand shield when an enemy or arrow gets close, lowers it when clear. */
public final class AutoShield extends Module {
    public final NumberSetting range = add(new NumberSetting("Range", 2, 8, 4, 0.5, "m"));

    private boolean holding;

    public AutoShield() {
        super("Auto Shield", "Blocks with your shield when danger is close.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (!(p.getOffhandItem().getItem() instanceof ShieldItem) || mc().screen != null) { release(); return; }
        boolean danger = false;
        for (Entity e : mc().level.entitiesForRendering()) {
            if (e.distanceTo(p) > range.value) continue;
            if (e instanceof AbstractArrow a && !a.onGround() && a.getOwner() != p) { danger = true; break; }
            if (Targets.valid(e, true, true, false, true)) { danger = true; break; }
        }
        if (danger && !mc().options.keyAttack.isDown()) { mc().options.keyUse.setDown(true); holding = true; }
        else release();
    }

    private void release() {
        if (holding) mc().options.keyUse.setDown(false);
        holding = false;
    }

    @Override
    public void onDisable() {
        release();
    }
}
