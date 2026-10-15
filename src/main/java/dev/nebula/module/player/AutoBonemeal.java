package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.util.Place;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Uses bone meal from your hotbar on growing crops around you. */
public final class AutoBonemeal extends Module {
    private int cooldown;

    public AutoBonemeal() {
        super("Auto Bonemeal", "Bone-meals nearby crops.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (cooldown-- > 0) return;
        var p = mc().player;
        int slot = Place.findItem(Items.BONE_MEAL);
        if (slot < 0) return;
        BlockPos c = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-4, -1, -4), c.offset(4, 1, 4))) {
            var st = mc().level.getBlockState(pos);
            if (!(st.getBlock() instanceof CropBlock crop) || crop.isMaxAge(st) || Vec3.atCenterOf(pos).distanceTo(p.getEyePosition()) > 4.5) continue;
            int prev = p.getInventory().selected;
            p.getInventory().selected = slot;
            mc().gameMode.useItemOn(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos.immutable(), false));
            p.getInventory().selected = prev;
            cooldown = 4;
            return;
        }
    }
}
