package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * No lava/fire damage. Damage is decided by the server, so the protection is real in singleplayer / LAN worlds you host
 * (the integrated server cancels fire and lava damage to you). On someone else's server only the client-side parts work:
 * no fire overlay, no burning visuals, and easier lava swimming.
 */
public final class AntiLava extends Module {
    public final BoolSetting hideOverlay = add(new BoolSetting("Hide Fire Overlay", true));
    public final BoolSetting lavaSwim = add(new BoolSetting("Lava Swim", true));
    public final NumberSetting rise = add(new NumberSetting("Rise Speed", 0.05, 0.4, 0.15, 0.01));

    public AntiLava() {
        super("Anti Lava", "No fire/lava damage (singleplayer/hosted), easy lava swimming.", Category.PLAYER);
    }

    @Override
    public String suffix() {
        return mc().hasSingleplayerServer() ? "SP" : "visual";
    }

    /** Integrated server side: cancel fire / lava / hot-floor damage aimed at the local player. */
    @SubscribeEvent
    public void onAttack(LivingAttackEvent e) {
        if (!isEnabled() || !(e.getEntity() instanceof ServerPlayer sp)) return;
        var me = mc().player;
        if (me == null || !mc().hasSingleplayerServer() || !sp.getUUID().equals(me.getUUID())) return;
        if (e.getSource().is(DamageTypeTags.IS_FIRE)) {
            e.setCanceled(true);
            sp.clearFire();
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderBlockScreenEffectEvent e) {
        if (isEnabled() && hideOverlay.get() && e.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.FIRE) e.setCanceled(true);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        p.clearFire(); // client-side visuals
        if (lavaSwim.get() && p.isEyeInFluid(FluidTags.LAVA) || lavaSwim.get() && p.isInLava()) {
            var v = p.getDeltaMovement();
            double vy = p.input.jumping ? rise.value : p.input.shiftKeyDown ? -rise.value : Math.max(v.y, -0.02);
            p.setDeltaMovement(v.x, vy, v.z);
        }
    }
}
