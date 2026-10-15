package dev.nebula.module.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.gui.Notifications;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.World3D;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.*;

/**
 * Finds ruined portals. Clients can't see structures, so this looks for crying obsidian
 * (it only generates naturally in ruined portals) and groups it into one marker per portal.
 */
public final class PortalFinder extends Module {
    private static final int COLOR = 0xFFB06BFF;

    public final NumberSetting range = add(new NumberSetting("Range", 2, 16, 10, 1, " ch"));
    public final BoolSetting beams = add(new BoolSetting("Beams", true));
    public final BoolSetting highlight = add(new BoolSetting("Highlight Blocks", true));
    public final BoolSetting notify = add(new BoolSetting("Notify", true));

    /** Per chunk: block positions tagged 0 crying obsidian, 1 obsidian, 2 netherrack/magma, 3 gold block. */
    private final Map<Long, List<long[]>> found = new HashMap<>();
    private final List<BlockPos> highlightBlocks = new ArrayList<>();
    private final ArrayDeque<Long> queue = new ArrayDeque<>();
    private final Set<Long> announced = new HashSet<>();
    private List<Vec3> portals = new ArrayList<>();
    private int ticks;

    public PortalFinder() {
        super("Portal Finder", "Finds ruined portals around you.", Category.WORLD);
    }

    @Override
    public String suffix() {
        return String.valueOf(portals.size());
    }

    @Override
    public void onEnable() {
        found.clear();
        queue.clear();
        portals = new ArrayList<>();
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || mc().level == null) return;
        ChunkPos c = p.chunkPosition();
        int r = range.i();
        if (queue.isEmpty()) {
            List<long[]> list = new ArrayList<>();
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) list.add(new long[]{ChunkPos.asLong(c.x + dx, c.z + dz), (long) dx * dx + (long) dz * dz});
            list.sort(Comparator.comparingLong(a -> a[1]));
            for (long[] l : list) queue.add(l[0]);
            found.keySet().removeIf(k -> Math.abs(ChunkPos.getX(k) - c.x) > r || Math.abs(ChunkPos.getZ(k) - c.z) > r);
        }
        for (int i = 0; i < 4 && !queue.isEmpty(); i++) scan(queue.poll());
        if (++ticks % 20 == 0) cluster();
    }

    private void scan(long key) {
        int cx = ChunkPos.getX(key), cz = ChunkPos.getZ(key);
        LevelChunk ch = mc().level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
        if (ch == null) { found.remove(key); return; }
        boolean nether = mc().level.dimension() == net.minecraft.world.level.Level.NETHER;
        List<long[]> out = new ArrayList<>();
        LevelChunkSection[] secs = ch.getSections();
        for (int si = 0; si < secs.length; si++) {
            LevelChunkSection s = secs[si];
            if (s == null || s.hasOnlyAir() || !s.maybeHas(st -> tag(st, nether) >= 0)) continue;
            int baseY = SectionPos.sectionToBlockCoord(ch.getSectionYFromSectionIndex(si));
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        int t = tag(s.getBlockState(x, y, z), nether);
                        if (t >= 0) out.add(new long[]{BlockPos.asLong((cx << 4) + x, baseY + y, (cz << 4) + z), t});
                    }
        }
        if (out.isEmpty()) found.remove(key);
        else found.put(key, out);
    }

    /** Netherrack/magma only count outside the Nether, where they're a portal giveaway. */
    private static int tag(net.minecraft.world.level.block.state.BlockState st, boolean nether) {
        if (st.is(Blocks.CRYING_OBSIDIAN)) return 0;
        if (st.is(Blocks.OBSIDIAN)) return 1;
        if (!nether && (st.is(Blocks.NETHERRACK) || st.is(Blocks.MAGMA_BLOCK))) return 2;
        if (st.is(Blocks.GOLD_BLOCK)) return 3;
        return -1;
    }

    /**
     * Groups tagged blocks within ~12 blocks. A group is a ruined portal if it has crying
     * obsidian, or obsidian together with netherrack/magma/gold around it.
     */
    private void cluster() {
        List<long[]> all = new ArrayList<>();
        for (var l : found.values()) all.addAll(l);
        List<Vec3> result = new ArrayList<>();
        List<BlockPos> hl = new ArrayList<>();
        boolean[] used = new boolean[all.size()];
        for (int i = 0; i < all.size(); i++) {
            if (used[i]) continue;
            BlockPos origin = BlockPos.of(all.get(i)[0]);
            int[] counts = new int[4];
            double sx = 0, sy = 0, sz = 0;
            int n = 0;
            List<BlockPos> members = new ArrayList<>();
            for (int j = i; j < all.size(); j++) {
                if (used[j]) continue;
                BlockPos bp = BlockPos.of(all.get(j)[0]);
                if (bp.distSqr(origin) > 144) continue;
                used[j] = true;
                int t = (int) all.get(j)[1];
                counts[t]++;
                if (t <= 1) {
                    members.add(bp);
                    sx += bp.getX(); sy += bp.getY(); sz += bp.getZ();
                    n++;
                }
            }
            boolean portal = counts[0] > 0 || (counts[1] >= 2 && counts[2] + counts[3] >= 2);
            if (!portal || n == 0) continue;
            hl.addAll(members);
            Vec3 center = new Vec3(sx / n + 0.5, sy / n, sz / n + 0.5);
            result.add(center);
            if (notify.get() && announced.add(BlockPos.containing(center.x / 16, 0, center.z / 16).asLong())) {
                int dist = (int) mc().player.position().distanceTo(center);
                Notifications.push("Ruined Portal", dist + "m away  " + (int) center.x + " " + (int) center.z, true);
            }
        }
        portals = result;
        highlightBlocks.clear();
        highlightBlocks.addAll(hl);
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        if (!isEnabled() || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || mc().player == null) return;
        Vec3 cam = e.getCamera().getPosition();
        Matrix4f m = e.getPoseStack().last().pose();
        if (highlight.get() && !highlightBlocks.isEmpty()) {
            RenderSystem.lineWidth(2f);
            BufferBuilder b = World3D.begin(VertexFormat.Mode.DEBUG_LINES, false);
            for (BlockPos bp : highlightBlocks) World3D.boxEdges(b, m, new AABB(bp).move(-cam.x, -cam.y, -cam.z), COLOR, 0.9f);
            World3D.end(b);
        }
        if (portals.isEmpty()) return;
        if (beams.get()) {
            BufferBuilder b = World3D.begin(VertexFormat.Mode.QUADS, true);
            for (Vec3 pt : portals) {
                double x = pt.x - cam.x, z = pt.z - cam.z, y0 = pt.y - cam.y, y1 = y0 + 140, w = 0.35;
                World3D.v(b, m, x - w, y0, z, COLOR, 0.45f); World3D.v(b, m, x + w, y0, z, COLOR, 0.45f);
                World3D.v(b, m, x + w, y1, z, COLOR, 0f); World3D.v(b, m, x - w, y1, z, COLOR, 0f);
                World3D.v(b, m, x, y0, z - w, COLOR, 0.45f); World3D.v(b, m, x, y0, z + w, COLOR, 0.45f);
                World3D.v(b, m, x, y1, z + w, COLOR, 0f); World3D.v(b, m, x, y1, z - w, COLOR, 0f);
            }
            World3D.end(b);
        }
        var me = mc().player.position();
        for (Vec3 pt : portals) {
            String text = "Ruined Portal  " + (int) me.distanceTo(pt) + "m";
            World3D.label(e.getPoseStack(), e.getCamera(), pt.x - cam.x, pt.y + 4 - cam.y, pt.z - cam.z, text, COLOR, 1.2f);
        }
    }
}
