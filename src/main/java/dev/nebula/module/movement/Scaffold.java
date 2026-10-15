package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Place;
import net.minecraft.core.BlockPos;

/** Places blocks from your hotbar under your feet as you walk, so you can bridge without looking. */
public final class Scaffold extends Module {
    public final NumberSetting extend = add(new NumberSetting("Extend", 0, 3, 0, 1, " blocks"));
    public final BoolSetting tower = add(new BoolSetting("Tower (Jump)", true));

    public Scaffold() {
        super("Scaffold", "Auto-places blocks beneath you.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        int slot = Place.findBlock();
        if (slot < 0) return;
        BlockPos under = BlockPos.containing(p.getX(), p.getY() - 0.5, p.getZ());
        Place.place(under, slot, false);
        if (extend.i() > 0 && dev.nebula.util.Move.moving(p)) {
            double[] d = dev.nebula.util.Move.dir(p);
            for (int i = 1; i <= extend.i(); i++) Place.place(BlockPos.containing(p.getX() + d[0] * i, p.getY() - 0.5, p.getZ() + d[1] * i), slot, false);
        }
        if (tower.get() && p.input.jumping && !dev.nebula.util.Move.moving(p) && p.onGround()) {
            p.jumpFromGround();
        }
    }
}
