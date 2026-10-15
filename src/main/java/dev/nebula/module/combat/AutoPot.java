package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Place;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;

/** Throws a splash healing potion at your feet when your health drops below the threshold. */
public final class AutoPot extends Module {
    public final NumberSetting health = add(new NumberSetting("Health", 2, 18, 8, 1, "hp"));
    public final NumberSetting delay = add(new NumberSetting("Delay", 5, 40, 15, 1, "t"));

    private int cooldown;

    public AutoPot() {
        super("Auto Pot", "Throws healing potions when low.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (cooldown > 0) { cooldown--; return; }
        if (p.getHealth() > health.value || mc().screen != null) return;
        int slot = Place.findHotbar(s -> s.is(Items.SPLASH_POTION)
                && PotionUtils.getMobEffects(s).stream().anyMatch(e -> e.getEffect() == MobEffects.HEAL));
        if (slot < 0) return;
        int prev = p.getInventory().selected;
        p.getInventory().selected = slot;
        // look straight down for this throw only
        p.connection.send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), 90f, p.onGround()));
        float pitch = p.getXRot();
        p.setXRot(90f);
        mc().gameMode.useItem(p, InteractionHand.MAIN_HAND);
        p.setXRot(pitch);
        p.getInventory().selected = prev;
        cooldown = delay.i();
    }
}
