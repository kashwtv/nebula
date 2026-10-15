package dev.nebula;

import dev.nebula.gui.ClickGui;
import dev.nebula.gui.HudEditor;
import dev.nebula.module.Module;
import dev.nebula.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Development-only test driver. Only registered when running from the dev workspace
 * (never in the released jar): reads commands from run/nebula_cmd.txt.
 */
public final class DevAutomation {
    private static final Deque<String> QUEUE = new ArrayDeque<>();
    private static int wait;
    private static int poll;

    private DevAutomation() {}

    private static boolean selfTested;

    /** Dev self-test (-Dnebula.selftest=true): once at the title screen, toggles every module on/off with no world loaded. */
    private static void selfTest(Minecraft mc) {
        if (selfTested || !(Boolean.getBoolean("nebula.selftest") || Files.exists(mc.gameDirectory.toPath().resolve("nebula_selftest"))) || mc.screen == null) return;
        selfTested = true;
        int fails = 0;
        for (Module m : ModuleManager.MODULES) {
            boolean was = m.isEnabled();
            try {
                m.setEnabled(!was);
                m.update();
                m.setEnabled(was);
            } catch (Throwable t) {
                fails++;
                Nebula.LOG.error("SELFTEST toggle failed: {}", m.name, t);
            }
        }
        try {
            for (Module m : ModuleManager.MODULES) if (m.name.equals("Auto Walk") || m.name.equals("Twerk") || m.name.equals("Dance")) m.setEnabled(true);
            ClientEvents.onLogout(null);
            long still = ModuleManager.MODULES.stream().filter(m -> (m.name.equals("Auto Walk") || m.name.equals("Twerk") || m.name.equals("Dance")) && m.isEnabled()).count();
            if (still > 0 || mc.options.keyUp.isDown() || mc.options.keyShift.isDown()) { fails++; Nebula.LOG.error("SELFTEST logout release failed"); }
            else Nebula.LOG.info("SELFTEST logout release OK");
        } catch (Throwable t) {
            fails++;
            Nebula.LOG.error("SELFTEST logout handler failed", t);
        }
        try {
            mc.setScreen(new ClickGui());
            Nebula.LOG.info("SELFTEST ClickGui opened");
        } catch (Throwable t) {
            fails++;
            Nebula.LOG.error("SELFTEST ClickGui failed", t);
        }
        Nebula.LOG.info("SELFTEST done: {} modules, {} failures", ModuleManager.MODULES.size(), fails);
    }

    /** Dev: run/nebula_structtest lists seeds; creates a fresh world per seed so LocateHarness can compare against /locate. */
    private static int stDbg;

    private static void structTest(Minecraft mc) {
        Path f = mc.gameDirectory.toPath().resolve("nebula_structtest");
        if (!Files.exists(f)) return;
        try {
            if (dev.nebula.structure.LocateHarness.DONE && mc.level != null) {
                dev.nebula.structure.LocateHarness.DONE = false;
                mc.level.disconnect();
                mc.clearLevel(new net.minecraft.client.gui.screens.GenericDirtMessageScreen(net.minecraft.network.chat.Component.literal("structtest")));
                mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
                return;
            }
            if (++stDbg % 200 == 0) Nebula.LOG.info("STRUCTTEST waiting: level={} screen={}", mc.level != null, mc.screen == null ? null : mc.screen.getClass().getName());
            if (mc.level != null || !(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen || mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen)) return;
            List<String> seeds = new java.util.ArrayList<>(Files.readAllLines(f).stream().map(String::trim).filter(x -> !x.isEmpty()).toList());
            if (seeds.isEmpty()) { Files.delete(f); Nebula.LOG.info("STRUCTTEST all seeds done"); mc.stop(); return; }
            String seed = seeds.remove(0);
            Files.write(f, seeds.isEmpty() ? List.of(" ") : seeds);
            Nebula.LOG.info("STRUCTTEST creating world for seed {}", seed);
            var settings = new net.minecraft.world.level.LevelSettings("structtest_" + seed + "_" + System.currentTimeMillis(), net.minecraft.world.level.GameType.CREATIVE, false,
                    net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(), net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new net.minecraft.world.level.levelgen.WorldOptions(Long.parseLong(seed), true, false),
                    ra -> ra.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL).value().createWorldDimensions());
        } catch (Exception ex) {
            Nebula.LOG.error("STRUCTTEST controller failed", ex);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        selfTest(mc);
        structTest(mc);
        if (mc.player == null || mc.level == null) return;
        if (++poll % 10 == 0) {
            Path f = mc.gameDirectory.toPath().resolve("nebula_cmd.txt");
            if (Files.exists(f)) {
                try {
                    List<String> lines = Files.readAllLines(f);
                    Files.delete(f);
                    QUEUE.addAll(lines);
                } catch (Exception ex) {
                    Nebula.LOG.warn("dev cmd read failed", ex);
                }
            }
        }
        if (wait > 0) { wait--; return; }
        while (!QUEUE.isEmpty() && wait == 0) {
            String line = QUEUE.poll().trim();
            if (line.isEmpty()) continue;
            try {
                run(mc, line);
            } catch (Exception ex) {
                Nebula.LOG.warn("dev cmd failed: {}", line, ex);
            }
        }
    }

    private static Module find(String name) {
        for (Module m : ModuleManager.MODULES) if (m.name.equalsIgnoreCase(name)) return m;
        return null;
    }

    private static void run(Minecraft mc, String line) throws Exception {
        String[] p = line.split(" ", 2);
        String arg = p.length > 1 ? p[1] : "";
        long win = mc.getWindow().getWindow();
        double gs = mc.getWindow().getGuiScale();
        switch (p[0]) {
            case "gui" -> mc.setScreen(new ClickGui());
            case "hud" -> mc.setScreen(new HudEditor());
            case "close" -> mc.setScreen(null);
            case "wait" -> wait = Integer.parseInt(arg);
            case "shot" -> Screenshot.grab(mc.gameDirectory, arg + ".png", mc.getMainRenderTarget(), c -> Nebula.LOG.info("dev shot {}", arg));
            case "on" -> { Module m = find(arg); if (m != null) m.setEnabled(true); }
            case "off" -> { Module m = find(arg); if (m != null) m.setEnabled(false); }
            case "expand" -> { Module m = find(arg); if (m != null) m.expanded = true; }
            case "collapse" -> { Module m = find(arg); if (m != null) m.expanded = false; }
            case "select" -> { if (mc.screen instanceof ClickGui g) g.devSelect(find(arg)); }
            case "cmd" -> { if (mc.player != null) mc.player.connection.sendCommand(arg); }
            case "look" -> {
                String[] a = arg.split(" ");
                mc.player.setYRot(Float.parseFloat(a[0]));
                mc.player.setXRot(Float.parseFloat(a[1]));
            }
            case "mouse" -> {
                // physical pixel coordinates
                String[] a = arg.split(" ");
                GLFW.glfwSetCursorPos(win, Double.parseDouble(a[0]), Double.parseDouble(a[1]));
            }
            case "click" -> {
                String[] a = arg.split(" ");
                double x = Double.parseDouble(a[0]), y = Double.parseDouble(a[1]);
                int b = a.length > 2 ? Integer.parseInt(a[2]) : 0;
                GLFW.glfwSetCursorPos(win, x, y);
                if (mc.screen != null) {
                    mc.screen.mouseClicked(x / gs, y / gs, b);
                    mc.screen.mouseReleased(x / gs, y / gs, b);
                }
            }
            case "log" -> Nebula.LOG.info("dev: {}", arg);
            case "info" -> { Module m = find(arg); Nebula.LOG.info("dev: info {} on={} suffix={}", arg, m != null && m.isEnabled(), m == null ? "?" : m.suffix()); }
            case "pos" -> Nebula.LOG.info("dev: pos {} y={} inWater={} onGround={}", arg, String.format("%.2f", mc.player.getY()), mc.player.isInWater(), mc.player.onGround());
            case "stats" -> Nebula.LOG.info("dev: stats {} hits={} kills={}", arg, dev.nebula.util.Combat.hits, dev.nebula.util.Combat.kills);
            case "set" -> {
                // set <Module>|<Setting>|<value>
                String[] a = arg.split("[|]");
                Module m = find(a[0]);
                if (m != null) for (var st : m.settings) if (st.name.equalsIgnoreCase(a[1])) st.load(com.google.gson.JsonParser.parseString(a[2]));
            }
            case "allon" -> {
                for (Module m : ModuleManager.MODULES) {
                    if (m.name.equals("Freecam") || m.name.equals("Auto Walk") || m.name.equals("Anti AFK")) continue;
                    try { m.setEnabled(true); } catch (Throwable t) { Nebula.LOG.error("dev: enable {} crashed", m.name, t); }
                }
            }
            case "alloff" -> {
                for (Module m : ModuleManager.MODULES) {
                    try { m.setEnabled(false); } catch (Throwable t) { Nebula.LOG.error("dev: disable {} crashed", m.name, t); }
                }
            }
            default -> Nebula.LOG.warn("dev: unknown command {}", line);
        }
    }
}
