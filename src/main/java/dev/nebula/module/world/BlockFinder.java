package dev.nebula.module.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.MultiSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.World3D;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
 * Highlights the ores you pick (diamonds, ancient debris, ...) through walls.
 * Loaded chunks are scanned a few per tick so it never stutters.
 */
public final class BlockFinder extends Module {
    private static final Map<String, Block[]> GROUPS = new LinkedHashMap<>();
    private static final Map<Block, Integer> COLORS = new HashMap<>();

    static {
        group("Diamond", 0xFF5CE1E6, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        group("Ancient Debris", 0xFFB0715A, Blocks.ANCIENT_DEBRIS);
        group("Emerald", 0xFF2EE66B, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        group("Gold", 0xFFFFD43B, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.RAW_GOLD_BLOCK);
        group("Iron", 0xFFD8AF93, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.RAW_IRON_BLOCK);
        group("Redstone", 0xFFFF3B3B, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        group("Lapis", 0xFF4466FF, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        group("Copper", 0xFFE0875A, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        group("Coal", 0xFF6A6A70, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        group("Quartz", 0xFFEDE6DA, Blocks.NETHER_QUARTZ_ORE);
        group("Nether Gold", 0xFFFFC23B, Blocks.NETHER_GOLD_ORE, Blocks.GILDED_BLACKSTONE);
        group("Amethyst", 0xFFB57BFF, Blocks.BUDDING_AMETHYST);
        group("Spawner", 0xFFFF6FA8, Blocks.SPAWNER);
    }

    private static void group(String name, int color, Block... blocks) {
        GROUPS.put(name, blocks);
        for (Block b : blocks) COLORS.put(b, color);
    }

    public final MultiSetting find = add(new MultiSetting("Blocks", GROUPS.keySet().toArray(new String[0]), "Diamond", "Ancient Debris"));
    public final NumberSetting range = add(new NumberSetting("Range", 2, 16, 8, 1, " ch"));
    public final NumberSetting scanSpeed = add(new NumberSetting("Scan Speed", 1, 10, 4, 1));
    public final NumberSetting maxShown = add(new NumberSetting("Max Shown", 50, 1500, 400, 50));
    public final BoolSetting fill = add(new BoolSetting("Fill", true));
    public final BoolSetting tracers = add(new BoolSetting("Tracers", false));
    public final BoolSetting throughWalls = add(new BoolSetting("Through Walls", true));

    private final Map<Long, List<BlockPos>> found = new HashMap<>();
    private final ArrayDeque<Long> queue = new ArrayDeque<>();
    private List<BlockPos> shown = new ArrayList<>();
    private Set<Block> targets = Set.of();
    private Set<String> lastFind = new HashSet<>();
    private int ticks, total;

    public BlockFinder() {
        super("Block Finder", "Finds diamonds, debris and other ores through walls.", Category.WORLD);
    }

    @Override
    public String suffix() {
        return String.valueOf(total);
    }

    private static Set<Block> resolve(Set<String> names) {
        Set<Block> s = new HashSet<>();
        for (String n : names) {
            Block[] b = GROUPS.get(n);
            if (b != null) s.addAll(Arrays.asList(b));
        }
        return s;
    }

    @Override
    public void onEnable() {
        found.clear();
        queue.clear();
        lastFind = new HashSet<>();
    }

    @Override
    public void onTick() {
        var p = mc().player;
        var level = mc().level;
        if (p == null || level == null) return;
        if (!find.selected.equals(lastFind)) {
            lastFind = new HashSet<>(find.selected);
            targets = resolve(lastFind);
            found.clear();
            queue.clear();
        }
        ChunkPos center = p.chunkPosition();
        int r = range.i();
        if (queue.isEmpty()) {
            List<long[]> list = new ArrayList<>();
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) list.add(new long[]{ChunkPos.asLong(center.x + dx, center.z + dz), (long) dx * dx + (long) dz * dz});
            list.sort(Comparator.comparingLong(a -> a[1]));
            for (long[] l : list) queue.add(l[0]);
            found.keySet().removeIf(k -> Math.abs(ChunkPos.getX(k) - center.x) > r || Math.abs(ChunkPos.getZ(k) - center.z) > r);
        }
        for (int i = 0; i < scanSpeed.i() && !queue.isEmpty(); i++) scan(queue.poll());
        if (++ticks % 10 == 0) refreshShown();
    }

    private void scan(long key) {
        var level = mc().level;
        int cx = ChunkPos.getX(key), cz = ChunkPos.getZ(key);
        LevelChunk c = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
        if (c == null) {
            found.remove(key);
            return;
        }
        List<BlockPos> out = new ArrayList<>();
        LevelChunkSection[] sections = c.getSections();
        for (int si = 0; si < sections.length; si++) {
            LevelChunkSection sec = sections[si];
            if (sec == null || sec.hasOnlyAir() || !sec.maybeHas(st -> targets.contains(st.getBlock()))) continue;
            int baseY = SectionPos.sectionToBlockCoord(c.getSectionYFromSectionIndex(si));
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        BlockState st = sec.getBlockState(x, y, z);
                        if (targets.contains(st.getBlock())) out.add(new BlockPos((cx << 4) + x, baseY + y, (cz << 4) + z));
                    }
        }
        if (out.isEmpty()) found.remove(key);
        else found.put(key, out);
    }

    private void refreshShown() {
        var p = mc().player;
        List<BlockPos> all = new ArrayList<>();
        for (List<BlockPos> l : found.values()) all.addAll(l);
        total = all.size();
        BlockPos me = p.blockPosition();
        all.sort(Comparator.comparingDouble(b -> b.distSqr(me)));
        shown = all.size() > maxShown.i() ? new ArrayList<>(all.subList(0, maxShown.i())) : all;
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        if (!isEnabled() || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || shown.isEmpty() || mc().level == null) return;
        Vec3 cam = e.getCamera().getPosition();
        Matrix4f m = e.getPoseStack().last().pose();
        boolean depth = !throughWalls.get();
        if (fill.get()) {
            BufferBuilder b = World3D.begin(VertexFormat.Mode.QUADS, depth);
            for (BlockPos bp : shown) World3D.boxQuads(b, m, box(bp, cam), color(bp), 0.18f);
            World3D.end(b);
        }
        RenderSystem.lineWidth(2f);
        BufferBuilder b = World3D.begin(VertexFormat.Mode.DEBUG_LINES, depth);
        var look = e.getCamera().getLookVector();
        int t = 0;
        for (BlockPos bp : shown) {
            int c = color(bp);
            World3D.boxEdges(b, m, box(bp, cam), c, 0.9f);
            if (tracers.get() && t++ < 30) {
                World3D.v(b, m, look.x(), look.y(), look.z(), c, 0.7f);
                World3D.v(b, m, bp.getX() + 0.5 - cam.x, bp.getY() + 0.5 - cam.y, bp.getZ() + 0.5 - cam.z, c, 0.7f);
            }
        }
        World3D.end(b);
    }

    private int color(BlockPos bp) {
        Block b = mc().level.getBlockState(bp).getBlock();
        return COLORS.getOrDefault(b, 0xFFEDEDEF);
    }

    private static AABB box(BlockPos bp, Vec3 cam) {
        return new AABB(bp).deflate(0.002).move(-cam.x, -cam.y, -cam.z);
    }
}
