package dev.nebula.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/** Block placing used by Scaffold, Surround, Auto Trap, Air Place and MLG. */
public final class Place {
    private Place() {}

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public static boolean replaceable(BlockPos pos) {
        return mc().level.getBlockState(pos).canBeReplaced();
    }

    /** Hotbar slot holding an item matching the filter, or -1. */
    public static int findHotbar(Predicate<ItemStack> filter) {
        var inv = mc().player.getInventory();
        if (filter.test(inv.getItem(inv.selected))) return inv.selected;
        for (int i = 0; i < 9; i++) if (filter.test(inv.getItem(i))) return i;
        return -1;
    }

    public static int findBlock() {
        return findHotbar(s -> s.getItem() instanceof BlockItem bi && bi.getBlock().defaultBlockState().isCollisionShapeFullBlock(mc().level, BlockPos.ZERO));
    }

    public static int findItem(Item item) {
        return findHotbar(s -> s.is(item));
    }

    /**
     * Places the block from {@code slot} at {@code pos} by clicking a solid neighbour.
     * With {@code airPlace} it may click the empty position itself.
     */
    public static boolean place(BlockPos pos, int slot, boolean airPlace) {
        var p = mc().player;
        var gm = mc().gameMode;
        if (p == null || gm == null || slot < 0 || !replaceable(pos)) return false;
        BlockHitResult hit = null;
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (replaceable(n)) continue;
            Direction face = d.getOpposite();
            Vec3 at = Vec3.atCenterOf(n).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            if (at.distanceToSqr(p.getEyePosition()) > 4.5 * 4.5) continue;
            hit = new BlockHitResult(at, face, n, false);
            break;
        }
        if (hit == null) {
            if (!airPlace) return false;
            hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        }
        int prev = p.getInventory().selected;
        p.getInventory().selected = slot;
        var r = gm.useItemOn(p, InteractionHand.MAIN_HAND, hit);
        if (r.consumesAction()) p.swing(InteractionHand.MAIN_HAND);
        p.getInventory().selected = prev;
        return r.consumesAction();
    }
}
