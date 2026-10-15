package dev.nebula.module.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.World3D;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.*;

/**
 * Highlights chests, barrels, shulkers and other storage through walls, and flags
 * clusters of storage as a likely base with a beam and a label.
 */
public final class StorageFinder extends Module {
    public final BoolSetting chests = add(new BoolSetting("Chests", true));
    public final BoolSetting barrels = add(new BoolSetting("Barrels", true));
    public final BoolSetting shulkers = add(new BoolSetting("Shulkers", true));
    public final BoolSetting enderChests = add(new BoolSetting("Ender Chests", true));
    public final BoolSetting hoppers = add(new BoolSetting("Hoppers / Droppers", false));
    public final BoolSetting furnaces = add(new BoolSetting("Furnaces", false));
    public final BoolSetting spawners = add(new BoolSetting("Spawners", true));
    public final BoolSetting fill = add(new BoolSetting("Fill", true));
    public final BoolSetting tracers = add(new BoolSetting("Tracers", false));
    public final BoolSetting baseFinder = add(new BoolSetting("Base Finder", true));
    public final NumberSetting baseThreshold = add(new NumberSetting("Base Threshold", 4, 60, 10, 1));

    private record Found(BlockPos pos, int color) {}

    private record Base(Vec3 center, int count) {}

    private List<Found> found = new ArrayList<>();
    private List<Base> bases = new ArrayList<>();
    private int ticks;

    public StorageFinder() {
        super("Storage Finder", "Finds chests and storage, and spots likely bases.", Category.WORLD);
        tracers.visibleWhen(() -> true);
        baseThreshold.visibleWhen(baseFinder::get);
    }

    @Override
    public String suffix() {
        return found.size() + (bases.isEmpty() ? "" : " · " + bases.size() + " base" + (bases.size() > 1 ? "s" : ""));
    }

    private int colorFor(BlockEntity be) {
        if (be instanceof ShulkerBoxBlockEntity) return shulkers.get() ? 0xFFFF6FA8 : 0;
        if (be instanceof EnderChestBlockEntity) return enderChests.get() ? 0xFFB57BFF : 0;
        if (be instanceof BarrelBlockEntity) return barrels.get() ? 0xFFC9935F : 0;
        if (be instanceof ChestBlockEntity) return chests.get() ? 0xFFFFD43B : 0;
        if (be instanceof HopperBlockEntity || be instanceof DispenserBlockEntity) return hoppers.get() ? 0xFF9AA3AD : 0;
        if (be instanceof AbstractFurnaceBlockEntity) return furnaces.get() ? 0xFF8B8B91 : 0;
        if (be instanceof SpawnerBlockEntity) return spawners.get() ? 0xFFFF4B4B : 0;
        return 0;
    }

    @Override
    public void onTick() {
        var p = mc().player;
        var level = mc().level;
        if (p == null || level == null || ++ticks % 20 != 1) return;
        ChunkPos c = p.chunkPosition();
        int r = mc().options.getEffectiveRenderDistance();
        List<Found> out = new ArrayList<>();
        Map<Long, List<BlockPos>> perChunk = new HashMap<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                LevelChunk ch = level.getChunkSource().getChunk(c.x + dx, c.z + dz, ChunkStatus.FULL, false);
                if (ch == null) continue;
                for (BlockEntity be : ch.getBlockEntities().values()) {
                    int col = colorFor(be);
                    if (col == 0) continue;
                    out.add(new Found(be.getBlockPos(), col));
                    if (!(be instanceof AbstractFurnaceBlockEntity) && !(be instanceof SpawnerBlockEntity))
                        perChunk.computeIfAbsent(ChunkPos.asLong(c.x + dx, c.z + dz), k -> new ArrayList<>()).add(be.getBlockPos());
                }
            }
        }
        found = out;
        bases = baseFinder.get() ? detectBases(perChunk) : List.of();
    }

    /** Sums storage over each 3x3 chunk area; dense areas become base markers. */
    private List<Base> detectBases(Map<Long, List<BlockPos>> perChunk) {
        List<Base> result = new ArrayList<>();
        List<long[]> candidates = new ArrayList<>();
        for (long key : perChunk.keySet()) {
            int cx = ChunkPos.getX(key), cz = ChunkPos.getZ(key);
            int sum = 0;
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    var l = perChunk.get(ChunkPos.asLong(cx + dx, cz + dz));
                    if (l != null) sum += l.size();
                }
            if (sum >= baseThreshold.i()) candidates.add(new long[]{key, sum});
        }
        candidates.sort((a, b) -> Long.compare(b[1], a[1]));
        for (long[] cand : candidates) {
            int cx = ChunkPos.getX(cand[0]), cz = ChunkPos.getZ(cand[0]);
            double sx = 0, sy = 0, sz = 0;
            int n = 0;
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    var l = perChunk.get(ChunkPos.asLong(cx + dx, cz + dz));
                    if (l == null) continue;
                    for (BlockPos bp : l) { sx += bp.getX(); sy += bp.getY(); sz += bp.getZ(); n++; }
                }
            Vec3 center = new Vec3(sx / n + 0.5, sy / n, sz / n + 0.5);
            boolean dup = false;
            for (Base b : result) if (b.center.distanceTo(center) < 48) { dup = true; break; }
            if (!dup) result.add(new Base(center, (int) cand[1]));
        }
        return result;
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        if (!isEnabled() || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || mc().player == null) return;
        Vec3 cam = e.getCamera().getPosition();
        Matrix4f m = e.getPoseStack().last().pose();
        if (!found.isEmpty()) {
            if (fill.get()) {
                BufferBuilder b = World3D.begin(VertexFormat.Mode.QUADS, false);
                for (Found f : found) World3D.boxQuads(b, m, box(f.pos, cam), f.color, 0.14f);
                World3D.end(b);
            }
            RenderSystem.lineWidth(2f);
            BufferBuilder b = World3D.begin(VertexFormat.Mode.DEBUG_LINES, false);
            var look = e.getCamera().getLookVector();
            for (Found f : found) {
                World3D.boxEdges(b, m, box(f.pos, cam), f.color, 0.9f);
                if (tracers.get()) {
                    World3D.v(b, m, look.x(), look.y(), look.z(), f.color, 0.6f);
                    World3D.v(b, m, f.pos.getX() + 0.5 - cam.x, f.pos.getY() + 0.5 - cam.y, f.pos.getZ() + 0.5 - cam.z, f.color, 0.6f);
                }
            }
            World3D.end(b);
        }
        if (!bases.isEmpty()) {
            BufferBuilder b = World3D.begin(VertexFormat.Mode.QUADS, true);
            for (Base base : bases) {
                double x = base.center.x - cam.x, z = base.center.z - cam.z;
                double y0 = base.center.y - cam.y, y1 = y0 + 160;
                float w = 0.35f;
                int c = 0xFFFFD43B;
                // two crossed vertical planes = a beam visible from any angle
                World3D.v(b, m, x - w, y0, z, c, 0.45f); World3D.v(b, m, x + w, y0, z, c, 0.45f);
                World3D.v(b, m, x + w, y1, z, c, 0f); World3D.v(b, m, x - w, y1, z, c, 0f);
                World3D.v(b, m, x, y0, z - w, c, 0.45f); World3D.v(b, m, x, y0, z + w, c, 0.45f);
                World3D.v(b, m, x, y1, z + w, c, 0f); World3D.v(b, m, x, y1, z - w, c, 0f);
            }
            World3D.end(b);
            var me = mc().player.position();
            for (Base base : bases) {
                int dist = (int) me.distanceTo(base.center);
                String text = "Possible base  " + base.count + " storage  " + dist + "m";
                World3D.label(e.getPoseStack(), e.getCamera(), base.center.x - cam.x, base.center.y + 3 - cam.y, base.center.z - cam.z, text, 0xFFFFD43B, 1.2f);
            }
        }
    }

    private static AABB box(BlockPos bp, Vec3 cam) {
        return new AABB(bp).deflate(0.06).move(-cam.x, -cam.y, -cam.z);
    }
}
