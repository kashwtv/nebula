package dev.nebula.module.utility;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;

/** Warns you when an arrow or trident is flying toward you. */
public final class ProjectileAlert extends Module {
    private int cooldown;

    public ProjectileAlert() {
        super("Projectile Alert", "Warns about incoming arrows.", Category.CLIENT);
    }

    @Override
    public void onTick() {
        if (cooldown-- > 0) return;
        var p = mc().player;
        for (Entity e : mc().level.entitiesForRendering()) {
            if (!(e instanceof AbstractArrow a) || a.onGround() || a.getOwner() == p || a.distanceTo(p) > 24) continue;
            var toMe = p.position().add(0, 1, 0).subtract(a.position()).normalize();
            var v = a.getDeltaMovement();
            if (v.lengthSqr() < 0.01 || v.normalize().dot(toMe) < 0.97) continue;
            p.displayClientMessage(Component.literal((a instanceof ThrownTrident ? "Trident" : "Arrow") + " incoming!").withStyle(ChatFormatting.RED), true);
            cooldown = 10;
            return;
        }
    }
}
