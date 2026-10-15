package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.TridentItem;

/**
 * Releases your bow the moment it reaches the charge you set. At full charge it's a
 * perfect-timing helper; at low charge it rapid-fires while you hold right click.
 */
public final class AutoBow extends Module {
    public final NumberSetting charge = add(new NumberSetting("Release At", 3, 20, 20, 1, "t"));

    public AutoBow() {
        super("Auto Bow", "Auto-releases arrows at the perfect charge.", Category.COMBAT);
    }

    @Override
    public String suffix() {
        return charge.i() >= 20 ? "Full" : charge.i() + "t";
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || mc().gameMode == null || !p.isUsingItem()) return;
        var item = p.getUseItem().getItem();
        int need = item instanceof TridentItem ? Math.max(10, charge.i()) : charge.i();
        if (!(item instanceof BowItem) && !(item instanceof TridentItem)) return;
        if (p.getTicksUsingItem() >= need) mc().gameMode.releaseUsingItem(p);
    }
}
