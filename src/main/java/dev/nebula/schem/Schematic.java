package dev.nebula.schem;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** A loaded build: block positions relative to its origin (air left out). Reads .litematic, .schem/.schematic (Sponge) and vanilla structure .nbt. */
public final class Schematic {
    public final String name;
    public final LinkedHashMap<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    public int sx, sy, sz;
    /** Block ids in the file this game doesn't know (and couldn't map), with counts. */
    public final Map<String, Integer> unknown = new TreeMap<>();
    /** Ids that were mapped to a vanilla block (e.g. diagonalfences:minecraft/spruce_fence -> minecraft:spruce_fence). */
    public final Map<String, String> remapped = new TreeMap<>();

    public Schematic(String name) { this.name = name; }

    public void put(int x, int y, int z, BlockState s) {
        if (s == null || s.isAir() || s.is(Blocks.STRUCTURE_VOID)) return;
        blocks.put(new BlockPos(x, y, z), s);
    }

    void bounds() {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : blocks.keySet()) {
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
        }
        if (blocks.isEmpty()) { sx = sy = sz = 0; return; }
        // normalise to start at 0,0,0
        if (minX != 0 || minY != 0 || minZ != 0) {
            var copy = new LinkedHashMap<>(blocks);
            blocks.clear();
            for (var e : copy.entrySet()) blocks.put(e.getKey().offset(-minX, -minY, -minZ), e.getValue());
        }
        sx = maxX - minX + 1; sy = maxY - minY + 1; sz = maxZ - minZ + 1;
    }

    /** Rotated (clockwise around Y) and/or mirrored copy, normalised to start at 0,0,0. */
    public Schematic transform(Rotation rot, Mirror mirror) {
        Schematic s = new Schematic(name);
        for (var e : blocks.entrySet()) {
            BlockPos p = e.getKey();
            int x = p.getX(), z = p.getZ();
            if (mirror == Mirror.LEFT_RIGHT) z = -z;
            else if (mirror == Mirror.FRONT_BACK) x = -x;
            int rx = x, rz = z;
            switch (rot) {
                case CLOCKWISE_90 -> { rx = -z; rz = x; }
                case CLOCKWISE_180 -> { rx = -x; rz = -z; }
                case COUNTERCLOCKWISE_90 -> { rx = z; rz = -x; }
                default -> {}
            }
            s.put(rx, p.getY(), rz, e.getValue().mirror(mirror).rotate(rot));
        }
        s.bounds();
        return s;
    }

    // ---------------------------------------------------------------- loading

    public static Schematic load(Path file) throws IOException {
        String fn = file.getFileName().toString();
        CompoundTag root;
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            root = NbtIo.readCompressed(in);
        } catch (IOException gz) {
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
                root = NbtIo.read(in); // uncompressed nbt
            }
        }
        String base = fn.replaceFirst("\\.[^.]+$", "");
        Schematic s = new Schematic(base);
        CUR.set(s);
        try {
        String low = fn.toLowerCase(Locale.ROOT);
        if (low.endsWith(".litematic") || root.contains("Regions")) readLitematic(root, s);
        else if (root.contains("palette") && root.contains("blocks")) readVanilla(root, s);
        else readSponge(root, s);
        } finally { CUR.remove(); }
        s.bounds();
        if (s.blocks.isEmpty()) throw new IOException("no blocks in " + fn);
        return s;
    }

    private static final ThreadLocal<Schematic> CUR = new ThreadLocal<>();

    /** Maps ids from mods that wrap vanilla blocks ("mod:minecraft/spruce_fence") to the vanilla block; records unknown ids. */
    private static String resolveId(String id) {
        var rl = net.minecraft.resources.ResourceLocation.tryParse(id);
        if (rl != null && BuiltInRegistries.BLOCK.containsKey(rl)) return id;
        if (rl != null && rl.getPath().contains("/")) {
            String path = rl.getPath();
            int i = path.indexOf('/');
            var alt = net.minecraft.resources.ResourceLocation.tryParse(path.substring(0, i) + ":" + path.substring(i + 1));
            if (alt == null || !BuiltInRegistries.BLOCK.containsKey(alt)) alt = net.minecraft.resources.ResourceLocation.tryParse("minecraft:" + path.substring(path.lastIndexOf('/') + 1));
            if (alt != null && BuiltInRegistries.BLOCK.containsKey(alt)) {
                Schematic s = CUR.get();
                if (s != null) s.remapped.put(id, alt.toString());
                return alt.toString();
            }
        }
        return null;
    }

    private static BlockState parseState(CompoundTag t) {
        String id = t.getString("Name");
        String res = resolveId(id);
        if (res == null) { Schematic s = CUR.get(); if (s != null) s.unknown.merge(id, 0, Integer::sum); return Blocks.STRUCTURE_VOID.defaultBlockState(); }
        if (!res.equals(id)) {
            t = t.copy();
            t.putString("Name", res);
            // drop properties the vanilla block doesn't have (diagonal fence north_east etc.)
            var block = BuiltInRegistries.BLOCK.get(new net.minecraft.resources.ResourceLocation(res));
            if (t.contains("Properties")) {
                CompoundTag props = t.getCompound("Properties").copy();
                for (String k : new ArrayList<>(props.getAllKeys())) if (block.getStateDefinition().getProperty(k) == null) props.remove(k);
                t.put("Properties", props);
            }
        }
        return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), t);
    }

    private static BlockState parseString(String s) {
        String id0 = s.contains("[") ? s.substring(0, s.indexOf('[')) : s;
        String res = resolveId(id0);
        if (res == null) { Schematic sc = CUR.get(); if (sc != null) sc.unknown.merge(id0, 0, Integer::sum); return Blocks.STRUCTURE_VOID.defaultBlockState(); }
        if (!res.equals(id0)) {
            Block b = BuiltInRegistries.BLOCK.get(new net.minecraft.resources.ResourceLocation(res));
            StringBuilder kept = new StringBuilder();
            if (s.contains("[")) for (String kv : s.substring(s.indexOf('[') + 1, s.lastIndexOf(']')).split(",")) {
                String k = kv.split("=")[0].trim();
                if (b.getStateDefinition().getProperty(k) != null) kept.append(kept.length() == 0 ? "" : ",").append(kv.trim());
            }
            s = res + (kept.length() > 0 ? "[" + kept + "]" : "");
        }
        try {
            return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), s, false).blockState();
        } catch (Exception e) {
            String id = s.contains("[") ? s.substring(0, s.indexOf('[')) : s;
            var rl = net.minecraft.resources.ResourceLocation.tryParse(id);
            Block b = rl == null ? Blocks.AIR : BuiltInRegistries.BLOCK.get(rl);
            return b.defaultBlockState();
        }
    }

    /** Vanilla structure block / jigsaw template. */
    static void readVanilla(CompoundTag root, Schematic s) {
        ListTag pal = root.getList("palette", Tag.TAG_COMPOUND);
        if (pal.isEmpty() && root.contains("palettes")) pal = root.getList("palettes", Tag.TAG_LIST).getList(0);
        BlockState[] states = new BlockState[pal.size()];
        for (int i = 0; i < pal.size(); i++) states[i] = parseState(pal.getCompound(i));
        ListTag bl = root.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < bl.size(); i++) {
            CompoundTag b = bl.getCompound(i);
            ListTag pos = b.getList("pos", Tag.TAG_INT);
            int st = b.getInt("state");
            if (st >= 0 && st < states.length) s.put(pos.getInt(0), pos.getInt(1), pos.getInt(2), states[st]);
        }
    }

    /** Sponge schematic v1/v2 (WorldEdit .schem) and v3 (nested "Schematic" / "Blocks"). */
    static void readSponge(CompoundTag root, Schematic s) throws IOException {
        if (root.contains("Schematic", Tag.TAG_COMPOUND)) root = root.getCompound("Schematic");
        int w = root.getShort("Width") & 0xFFFF, h = root.getShort("Height") & 0xFFFF, l = root.getShort("Length") & 0xFFFF;
        CompoundTag palette; byte[] data;
        if (root.contains("Blocks", Tag.TAG_COMPOUND)) { // v3
            CompoundTag b = root.getCompound("Blocks");
            palette = b.getCompound("Palette");
            data = b.getByteArray("Data");
        } else if (root.contains("Palette", Tag.TAG_COMPOUND)) {
            palette = root.getCompound("Palette");
            data = root.getByteArray("BlockData");
        } else throw new IOException("Old MCEdit .schematic (numeric ids) isn't supported; re-save it as .schem in WorldEdit");
        BlockState[] states = new BlockState[palette.size() + 1];
        Map<Integer, BlockState> byId = new HashMap<>();
        for (String k : palette.getAllKeys()) byId.put(palette.getInt(k), parseString(k));
        int idx = 0, i = 0;
        while (i < data.length) {
            int v = 0, shift = 0, b;
            do { b = data[i++]; v |= (b & 0x7F) << shift; shift += 7; } while ((b & 0x80) != 0 && i < data.length);
            int y = idx / (w * l), rem = idx % (w * l), z = rem / w, x = rem % w;
            s.put(x, y, z, byId.get(v));
            idx++;
        }
    }

    /** Litematica: every region, tightly packed long array (entries may span two longs). */
    static void readLitematic(CompoundTag root, Schematic s) {
        CompoundTag regions = root.getCompound("Regions");
        for (String rn : regions.getAllKeys()) {
            CompoundTag r = regions.getCompound(rn);
            CompoundTag pos = r.getCompound("Position"), size = r.getCompound("Size");
            int px = pos.getInt("x"), py = pos.getInt("y"), pz = pos.getInt("z");
            int sx = size.getInt("x"), sy = size.getInt("y"), sz = size.getInt("z");
            // negative size: region extends backwards from Position
            int ox = px + (sx < 0 ? sx + 1 : 0), oy = py + (sy < 0 ? sy + 1 : 0), oz = pz + (sz < 0 ? sz + 1 : 0);
            int ax = Math.abs(sx), ay = Math.abs(sy), az = Math.abs(sz);
            ListTag pal = r.getList("BlockStatePalette", Tag.TAG_COMPOUND);
            BlockState[] states = new BlockState[pal.size()];
            for (int i = 0; i < pal.size(); i++) states[i] = parseState(pal.getCompound(i));
            long[] arr = r.getLongArray("BlockStates");
            int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, states.length - 1)));
            long mask = (1L << bits) - 1;
            long vol = (long) ax * ay * az;
            for (long idx = 0; idx < vol; idx++) {
                long start = idx * bits;
                int si = (int) (start >> 6), off = (int) (start & 63);
                if (si >= arr.length) break;
                long v = arr[si] >>> off;
                if (off + bits > 64 && si + 1 < arr.length) v |= arr[si + 1] << (64 - off);
                int id = (int) (v & mask);
                int x = (int) (idx % ax), z = (int) ((idx / ax) % az), y = (int) (idx / ((long) ax * az));
                if (id < states.length) s.put(ox + x, oy + y, oz + z, states[id]);
            }
        }
    }

    // ---------------------------------------------------------------- presets

    public static final List<String> PRESETS = List.of("cobble_hut", "nether_portal", "pillar", "pixel_heart", "bridge");

    public static Schematic preset(String id) {
        Schematic s = new Schematic("preset:" + id);
        BlockState cob = Blocks.COBBLESTONE.defaultBlockState(), plank = Blocks.OAK_PLANKS.defaultBlockState(),
                obs = Blocks.OBSIDIAN.defaultBlockState(), glass = Blocks.GLASS.defaultBlockState();
        switch (id) {
            case "cobble_hut" -> { // 5x4x5 with a door gap and window
                for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
                    s.put(x, 0, z, cob);
                    s.put(x, 4, z, plank);
                    boolean wall = x == 0 || x == 4 || z == 0 || z == 4;
                    for (int y = 1; y <= 3; y++) if (wall) {
                        if (x == 2 && z == 0 && y <= 2) continue; // door
                        s.put(x, y, z, (y == 2 && z == 2 && (x == 0 || x == 4)) ? glass : cob);
                    }
                }
            }
            case "nether_portal" -> {
                for (int x = 0; x < 4; x++) { s.put(x, 0, 0, obs); s.put(x, 4, 0, obs); }
                for (int y = 1; y < 4; y++) { s.put(0, y, 0, obs); s.put(3, y, 0, obs); }
            }
            case "pillar" -> { for (int y = 0; y < 10; y++) s.put(0, y, 0, cob); }
            case "bridge" -> { for (int z = 0; z < 16; z++) for (int x = 0; x < 3; x++) s.put(x, 0, z, cob); }
            case "pixel_heart" -> {
                String[] rows = {".XX.XX.", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X..."};
                BlockState red = Blocks.RED_WOOL.defaultBlockState();
                for (int r = 0; r < rows.length; r++) for (int c = 0; c < rows[r].length(); c++)
                    if (rows[r].charAt(c) == 'X') s.put(c, rows.length - 1 - r, 0, red);
            }
            default -> { return null; }
        }
        s.bounds();
        return s;
    }
}
