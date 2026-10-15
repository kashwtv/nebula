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

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
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
