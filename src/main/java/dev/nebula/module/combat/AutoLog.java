package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** Disconnects you before you die. Turns itself off so you can rejoin. */
public final class AutoLog extends Module {
    public final NumberSetting health = add(new NumberSetting("Health", 1, 19, 6, 1, " hp"));
    public final BoolSetting noTotems = add(new BoolSetting("Only Without Totems", false));
    public final BoolSetting playerNearby = add(new BoolSetting("Only If Player Near", false));

    public AutoLog() {
        super("Auto Log", "Leaves the server when your health gets too low.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        var c = mc().getConnection();
        if (p == null || c == null || p.isCreative() || p.isDeadOrDying()) return;
        if (p.getHealth() + p.getAbsorptionAmount() > health.value) return;
        if (noTotems.get()) {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) return;
        }
        if (playerNearby.get()) {
            boolean near = false;
            for (var e : mc().level.players()) if (e != p && e.distanceTo(p) < 32) near = true;
            if (!near) return;
        }
        setEnabled(false);
        c.getConnection().disconnect(Component.literal("[Nebula] Auto Log: " + String.format("%.1f", p.getHealth()) + " hp"));
    }
}
