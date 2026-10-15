package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;

/**
 * Walk on water (and lava if you want). "Solid" lets you stand, walk and jump on the
 * surface like a floor; "Dolphin" bobs you up to the surface while you swim; "Bounce"
 * hops you along the top. Hold sneak to sink as normal.
 */
public final class Jesus extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Solid", "Solid", "Dolphin", "Bounce"));
    public final BoolSetting lava = add(new BoolSetting("Lava", true));
    public final NumberSetting rise = add(new NumberSetting("Rise Speed", 0.05, 0.4, 0.11, 0.01));

    public Jesus() {
        super("Jesus", "Walk on top of water and lava.", Category.MOVEMENT);
    }

    @Override
    public String suffix() {
        return mode.get();
    }

    private boolean liquid(FluidState f) {
        return f.is(FluidTags.WATER) || (lava.get() && f.is(FluidTags.LAVA));
    }

    @Override
    public void onTick() {
        var p = mc().player;
        var level = mc().level;
        if (p == null || level == null || p.isShiftKeyDown() || p.isPassenger() || p.getAbilities().flying || p.isFallFlying()) return;
        var v = p.getDeltaMovement();
        boolean inLiquid = p.isInWater() || (lava.get() && p.isInLava());
        double depth = Math.max(p.getFluidHeight(FluidTags.WATER), lava.get() ? p.getFluidHeight(FluidTags.LAVA) : 0);

        if (mode.is("Dolphin")) {
            if (inLiquid && v.y < rise.value) p.setDeltaMovement(v.x, rise.value, v.z);
            return;
        }
        if (mode.is("Bounce")) {
            if (inLiquid && depth < 0.6) p.setDeltaMovement(v.x, 0.3, v.z);
            else if (inLiquid) p.setDeltaMovement(v.x, Math.max(v.y, rise.value), v.z);
            if (inLiquid) p.fallDistance = 0;
            return;
        }

        // Solid: if we're in the liquid near the top, push up onto the surface
        if (inLiquid) {
            if (depth < 0.6) p.setDeltaMovement(v.x, 0.15, v.z);
            else p.setDeltaMovement(v.x, Math.max(v.y, rise.value), v.z);
            p.fallDistance = 0;
            return;
        }
        // standing just above the liquid surface: treat it as ground (unless jumping)
        FluidState below = level.getFluidState(BlockPos.containing(p.getX(), p.getY() - 0.05, p.getZ()));
        if (liquid(below) && v.y <= 0 && !p.input.jumping) {
            p.setDeltaMovement(v.x, 0, v.z);
            p.fallDistance = 0;
            p.setOnGround(true);
        }
    }
}
