package dev.nebula.schem;

import dev.nebula.Nebula;
import dev.nebula.module.world.AutoBuild;
import dev.nebula.module.world.StructureFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.file.*;
import java.util.*;

/** Dev only: run/nebula_buildtest -> world, portal room check, schematic loader round trips, AutoBuild placing obsidian. Results in nebula_buildtest_results.txt. */
public final class BuildHarness {
    private BuildHarness() {}
    private static final List<String> OUT = new ArrayList<>();
    private static int stage, ticks;

    private static Path dir() { return Minecraft.getInstance().gameDirectory.toPath(); }
    private static void log(String s) { OUT.add(s); Nebula.LOG.info("BUILDTEST " + s); }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) {
        if (!Files.exists(dir().resolve("nebula_buildtest"))) return;
        var lvl = e.getServer().overworld();
        try {
            var holder = lvl.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(BuiltinStructures.STRONGHOLD);
            for (int[] o : new int[][]{{0, 0}, {3000, -2000}}) {
                var real = lvl.getChunkSource().getGenerator().findNearestMapStructure(lvl, HolderSet.direct(holder), new BlockPos(o[0], 64, o[1]), 100, false);
                BlockPos sp = real.getFirst();
                int[] r = StructureFinder.portalFromStart(lvl, sp.getX() >> 4, sp.getZ() >> 4);
                log("portal: stronghold " + sp.getX() + "," + sp.getZ() + " -> " + (r == null ? "NONE" : "room " + r[0] + "," + r[1] + "," + r[2] + " frames=" + r[3] + " eyes=" + r[4])
                        + (r != null && r[3] == 12 ? " PASS" : " FAIL"));
            }
        } catch (Throwable t) { log("portal FAIL " + t); Nebula.LOG.error("buildtest portal", t); }
        try {
            Schematic ref = Schematic.preset("cobble_hut");
            Path d = AutoBuild.folder();
            writeVanilla(ref, d.resolve("t_hut.nbt"));
            writeSponge(ref, d.resolve("t_hut.schem"));
            writeLitematic(ref, d.resolve("t_hut.litematic"));
            writeLitematic(Schematic.preset("nether_portal"), d.resolve("t_portal.litematic"));
            for (String f : List.of("t_hut.nbt", "t_hut.schem", "t_hut.litematic")) {
                Schematic s = Schematic.load(d.resolve(f));
                log("load " + f + ": " + s.blocks.size() + " blocks, " + s.sx + "x" + s.sy + "x" + s.sz + (s.blocks.equals(ref.blocks) ? " PASS" : " FAIL"));
            }
            var rot = ref.transform(net.minecraft.world.level.block.Rotation.CLOCKWISE_90, net.minecraft.world.level.block.Mirror.NONE);
            var back = rot.transform(net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90, net.minecraft.world.level.block.Mirror.NONE);
            log("rotate 90 + back: " + (back.blocks.equals(ref.blocks) ? "PASS" : "FAIL"));
        } catch (Throwable t) { log("schematic FAIL " + t); Nebula.LOG.error("buildtest schem", t); }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !Files.exists(dir().resolve("nebula_buildtest"))) return;
        var mc = Minecraft.getInstance();
        try {
            if (stage == 0 && mc.level == null && mc.screen != null && ++ticks > 40) {
                ticks = 0;
                stage = 1;
                var settings = new net.minecraft.world.level.LevelSettings("buildtest_" + System.currentTimeMillis(), net.minecraft.world.level.GameType.SURVIVAL, false,
                        net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(), net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new net.minecraft.world.level.levelgen.WorldOptions(12345L, true, false),
                        ra -> ra.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL).value().createWorldDimensions());
            } else if (stage == 1 && mc.player != null && ++ticks > 60) {
                stage = 2; ticks = 0;
                var srv = mc.getSingleplayerServer();
                srv.execute(() -> {
                    var lvl = srv.overworld();
                    for (int x = -6; x <= 8; x++) for (int z = -6; z <= 8; z++) {
                        lvl.setBlockAndUpdate(new BlockPos(x, 200, z), Blocks.STONE.defaultBlockState());
                        for (int y = 201; y < 208; y++) lvl.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                    var sp = srv.getPlayerList().getPlayers().get(0);
                    sp.teleportTo(lvl, 2.5, 201, 0.5, 0, 0);
                    sp.getInventory().clearContent();
                    sp.getInventory().setItem(20, new ItemStack(Items.OBSIDIAN, 64)); // main inventory: tests the swap
                });
            } else if (stage == 2 && ++ticks > 40) {
                stage = 3; ticks = 0;
                var ab = AutoBuild.get();
                log(ab.load("t_portal.litematic"));
                ab.origin = new BlockPos(1, 201, 2);
                ab.setEnabled(true);
            } else if (stage == 3 && ++ticks > 200) {
                stage = 4;
                int n = 0;
                var lvl = mc.getSingleplayerServer().overworld();
                for (var p : Schematic.preset("nether_portal").blocks.keySet())
                    if (lvl.getBlockState(p.offset(1, 201, 2)).is(Blocks.OBSIDIAN)) n++;
                log("autobuild placed " + n + "/14 obsidian, module " + (AutoBuild.get().isEnabled() ? "still on" : "finished") + (n == 14 ? " PASS" : " FAIL"));
                var fl = (dev.nebula.module.movement.Flight) dev.nebula.module.ModuleManager.MODULES.stream().filter(m -> m instanceof dev.nebula.module.movement.Flight).findFirst().get();
                for (String mode : List.of("Vanilla", "Creative", "Motion", "Glide", "Jetpack")) {
                    fl.mode.load(new com.google.gson.JsonPrimitive(mode));
                    fl.setEnabled(true); for (int i = 0; i < 5; i++) fl.onTick(); fl.setEnabled(false);
                }
                fl.setEnabled(true);
                dev.nebula.ClientEvents.onLogout(null);
                log("flight modes ticked; logout disables flight: " + (!fl.isEnabled() && !mc.player.getAbilities().mayfly ? "PASS" : "FAIL"));
                var al = dev.nebula.module.ModuleManager.MODULES.stream().filter(m -> m instanceof dev.nebula.module.player.AntiLava).findFirst().get();
                var srv = mc.getSingleplayerServer();
                float[] hp = srv.submit(() -> {
                    var sp = srv.getPlayerList().getPlayers().get(0);
                    sp.setHealth(20); float a;
                    al.setEnabled(true); sp.invulnerableTime = 0; sp.hurt(sp.damageSources().lava(), 4); a = sp.getHealth();
                    al.setEnabled(false); sp.invulnerableTime = 0; sp.hurt(sp.damageSources().lava(), 4);
                    return new float[]{a, sp.getHealth()};
                }).get();
                log("anti lava: health on=" + hp[0] + " off=" + hp[1] + (hp[0] == 20 && hp[1] < 20 ? " PASS" : " FAIL"));
                Files.write(dir().resolve("nebula_buildtest_results.txt"), OUT);
                Files.delete(dir().resolve("nebula_buildtest"));
                mc.stop();
            }
        } catch (Throwable t) {
            Nebula.LOG.error("buildtest", t);
            try { OUT.add("ERROR " + t); Files.write(dir().resolve("nebula_buildtest_results.txt"), OUT); Files.deleteIfExists(dir().resolve("nebula_buildtest")); } catch (Exception ignored) {}
            mc.stop();
        }
    }

    // --------- test writers
    private static List<BlockState> palette(Schematic s) {
        List<BlockState> p = new ArrayList<>(); p.add(Blocks.AIR.defaultBlockState());
        for (var st : s.blocks.values()) if (!p.contains(st)) p.add(st);
        return p;
    }
    private static BlockState at(Schematic s, int x, int y, int z) {
        return s.blocks.getOrDefault(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
    }

    static void writeVanilla(Schematic s, Path f) throws Exception {
        var pal = palette(s); CompoundTag root = new CompoundTag(); ListTag pl = new ListTag(), bl = new ListTag();
        for (var st : pal) pl.add(NbtUtils.writeBlockState(st));
        for (var e : s.blocks.entrySet()) {
            CompoundTag b = new CompoundTag(); ListTag pos = new ListTag();
            pos.add(IntTag.valueOf(e.getKey().getX())); pos.add(IntTag.valueOf(e.getKey().getY())); pos.add(IntTag.valueOf(e.getKey().getZ()));
            b.put("pos", pos); b.putInt("state", pal.indexOf(e.getValue())); bl.add(b);
        }
        root.put("palette", pl); root.put("blocks", bl);
        NbtIo.writeCompressed(root, f.toFile());
    }

    static void writeSponge(Schematic s, Path f) throws Exception {
        var pal = palette(s); CompoundTag root = new CompoundTag(), p = new CompoundTag();
        for (int i = 0; i < pal.size(); i++) {
            String str = net.minecraft.commands.arguments.blocks.BlockStateParser.serialize(pal.get(i));
            p.putInt(str, i);
        }
        var bytes = new java.io.ByteArrayOutputStream();
        for (int y = 0; y < s.sy; y++) for (int z = 0; z < s.sz; z++) for (int x = 0; x < s.sx; x++) {
            int v = pal.indexOf(at(s, x, y, z));
            while ((v & ~0x7F) != 0) { bytes.write((v & 0x7F) | 0x80); v >>>= 7; } bytes.write(v);
        }
        root.putInt("Version", 2); root.putShort("Width", (short) s.sx); root.putShort("Height", (short) s.sy); root.putShort("Length", (short) s.sz);
        root.put("Palette", p); root.putByteArray("BlockData", bytes.toByteArray());
        NbtIo.writeCompressed(root, f.toFile());
    }

    static void writeLitematic(Schematic s, Path f) throws Exception {
        var pal = palette(s);
        int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(pal.size() - 1));
        long vol = (long) s.sx * s.sy * s.sz;
        long[] arr = new long[(int) ((vol * bits + 63) / 64)];
        long idx = 0;
        for (int y = 0; y < s.sy; y++) for (int z = 0; z < s.sz; z++) for (int x = 0; x < s.sx; x++, idx++) {
            long v = pal.indexOf(at(s, x, y, z)); long start = idx * bits; int si = (int) (start >> 6), off = (int) (start & 63);
            arr[si] |= v << off;
            if (off + bits > 64) arr[si + 1] |= v >>> (64 - off);
        }
        CompoundTag r = new CompoundTag(), pos = new CompoundTag(), size = new CompoundTag();
        pos.putInt("x", 0); pos.putInt("y", 0); pos.putInt("z", 0);
        size.putInt("x", s.sx); size.putInt("y", s.sy); size.putInt("z", s.sz);
        ListTag pl = new ListTag(); for (var st : pal) pl.add(NbtUtils.writeBlockState(st));
        r.put("Position", pos); r.put("Size", size); r.put("BlockStatePalette", pl); r.putLongArray("BlockStates", arr);
        CompoundTag regions = new CompoundTag(); regions.put(s.name, r);
        CompoundTag root = new CompoundTag(); root.put("Regions", regions); root.putInt("Version", 6);
        NbtIo.writeCompressed(root, f.toFile());
    }
}
