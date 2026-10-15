package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.tags.BlockTags;

/** Go faster on ice, packed ice and blue ice. */
public final class IceSpeed extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed", 0.3, 1.5, 0.6, 0.05));

    public IceSpeed() {
        super("Ice Speed", "Move faster on ice.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (!p.onGround() || !dev.nebula.util.Move.moving(p)) return;
        var below = p.level().getBlockState(p.getOnPos());
        if (!below.is(BlockTags.ICE)) return;
        dev.nebula.util.Move.easeSpeed(p, speed.value, 0.2);
    }
}
