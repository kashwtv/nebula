package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.InteractionHand;

import java.util.concurrent.ThreadLocalRandom;

/** Small random actions every so often so you don't get kicked for being AFK. */
public final class AntiAfk extends Module {
    public final NumberSetting interval = add(new NumberSetting("Interval", 5, 120, 30, 5, "s"));
    public final BoolSetting jump = add(new BoolSetting("Jump", true));
    public final BoolSetting turn = add(new BoolSetting("Turn", true));
    public final BoolSetting swing = add(new BoolSetting("Swing", true));

    private int ticks;

    public AntiAfk() {
        super("Anti AFK", "Stops AFK kicks with small random actions.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        if (++ticks < interval.value * 20) return;
        ticks = ThreadLocalRandom.current().nextInt(0, 40);
        var r = ThreadLocalRandom.current();
        if (jump.get() && p.onGround()) p.jumpFromGround();
        if (turn.get()) p.setYRot(p.getYRot() + (float) r.nextDouble(-35, 35));
        if (swing.get()) p.swing(InteractionHand.MAIN_HAND);
    }
}
