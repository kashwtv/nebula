package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Place;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Harvests fully-grown crops around you and replants seeds on empty farmland. */
public final class AutoFarm extends Module {
    public final NumberSetting range = add(new NumberSetting("Range", 2, 4.5, 4, 0.5, "m"));
    public final BoolSetting replant = add(new BoolSetting("Replant", true));

    private int cooldown;

    public AutoFarm() {
        super("Auto Farm", "Harvests and replants crops.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (cooldown-- > 0) return;
        var p = mc().player;
        var lvl = mc().level;
        int r = (int) Math.ceil(range.value);
        BlockPos c = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-r, -1, -r), c.offset(r, 1, r))) {
            if (Vec3.atCenterOf(pos).distanceTo(p.getEyePosition()) > range.value) continue;
            var st = lvl.getBlockState(pos);
            if (st.getBlock() instanceof CropBlock crop && crop.isMaxAge(st)) {
                mc().gameMode.startDestroyBlock(pos.immutable(), Direction.UP);
                p.swing(InteractionHand.MAIN_HAND);
                cooldown = 2;
                return;
            }
            if (replant.get() && st.is(Blocks.FARMLAND) && lvl.getBlockState(pos.above()).isAir()) {
                int slot = Place.findHotbar(s -> s.getItem() instanceof ItemNameBlockItem bi && bi.getBlock() instanceof CropBlock);
                if (slot < 0) continue;
                int prev = p.getInventory().selected;
                p.getInventory().selected = slot;
                mc().gameMode.useItemOn(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos.immutable(), false));
                p.getInventory().selected = prev;
                cooldown = 2;
                return;
            }
        }
    }
}
