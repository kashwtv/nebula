package dev.nebula.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.config.ConfigIO;
import dev.nebula.module.world.AutoBuild;
import dev.nebula.render.R2D;
import dev.nebula.render.Theme;
import dev.nebula.schem.Schematic;
import dev.nebula.util.Col;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/** Auto Build panel: schematic library, rotatable isometric preview, materials, and build controls. */
public final class AutoBuildScreen extends NScreen {
    private List<String> files = AutoBuild.files();
    private float yaw = 45, pitch = 30, zoom = 1;
    private boolean rotating;
    private float lastX, lastY;
    private float listScroll, listMax, matScroll, matMax;
    private float px0, py0, pw0, ph0; // preview rect
    private String toast;
    private long toastAt;

    public AutoBuildScreen() { super("Auto Build"); }

    private void say(String s) { toast = s; toastAt = System.currentTimeMillis(); }

    @Override
    protected void draw(GuiGraphics g, float mx, float my) {
        var ab = AutoBuild.get();
        float ww = Math.min(vw - 24, 640), wh = Math.min(vh - 24, 372);
        float wx = (vw - ww) / 2, wy = (vh - wh) / 2;
        window(g, wx, wy, ww, wh);
        R2D.bold(g, "Auto Build", wx + 14, wy + 12, Theme.TEXT);
        String st = ab.isEnabled() ? "Building" : ab.active == null ? "No schematic loaded" : "Ready";
        float stw = R2D.width(st) + 14;
        R2D.rect(g, wx + 18 + R2D.boldWidth("Auto Build"), wy + 10.5f, stw, 12, 6, ab.isEnabled() ? Col.alpha(Theme.GREEN, 0.18f) : Theme.TRACK);
        R2D.text(g, st, wx + 25 + R2D.boldWidth("Auto Build"), wy + 13, ab.isEnabled() ? Theme.GREEN : Theme.MUTED);
        float hx = wx + ww - 14;
        hx -= pill(g, "Add schematic", hx - (R2D.width("Add schematic") + 16), wy + 8, 0, true, mx, my, this::addSchematic) + 4;
        hx -= pill(g, "Open folder", hx - (R2D.width("Open folder") + 16), wy + 8, 0, false, mx, my, () -> Util.getPlatform().openFile(AutoBuild.folder().toFile())) + 4;
        pill(g, "Refresh", hx - (R2D.width("Refresh") + 16), wy + 8, 0, false, mx, my, () -> files = AutoBuild.files());

        float top = wy + 36, h = wh - 50;
        // ---------------- library
        float lx = wx + 14, lw = 150;
        card(g, lx - 6, top - 6, lw + 12, h + 6);
        header(g, "Schematics", files.size() + " files", lx, top, lw);
        float lt = top + 13, lb = top + h - 6;
        R2D.pushScissor(g, lx - 4, lt, lw + 8, lb - lt);
        float y = lt - listScroll;
        for (String f : files) { y = row(g, f, f.replaceAll("\\.[^.]+$", ""), ext(f), ab, lx, y, lw, mx, my, lt, lb); }
        y += 4;
        R2D.text(g, "Presets", lx, y + 2, Theme.DIM);
        y += 14;
        for (String p : Schematic.PRESETS) y = row(g, "preset:" + p, pretty(p), "preset", ab, lx, y, lw, mx, my, lt, lb);
        if (files.isEmpty()) R2D.text(g, "", lx, y, Theme.DIM);
        listMax = Math.max(0, y + listScroll - lb);
        R2D.popScissor(g);

        // ---------------- preview
        float cx = lx + lw + 18, cw = ww - (cx - wx) - 14 - 186;
        card(g, cx - 6, top - 6, cw + 12, h + 6);
        var sch = ab.active;
        header(g, sch == null ? "Preview" : pretty(sch.name.replace("preset:", "")), sch == null ? null : sch.sx + " x " + sch.sy + " x " + sch.sz + "   " + sch.blocks.size() + " blocks", cx, top, cw);
        px0 = cx; py0 = top + 14; pw0 = cw; ph0 = h - 14 - 50;
        R2D.rect(g, px0, py0, pw0, ph0, 6, 0xFF0F0F11);
        if (sch == null) R2D.textCentered(g, "Pick a schematic on the left", px0 + pw0 / 2, py0 + ph0 / 2 - 4, Theme.DIM);
        else {
            iso(g, sch, px0, py0, pw0, ph0);
            R2D.text(g, "drag to rotate · scroll to zoom", px0 + 6, py0 + ph0 - 12, Theme.DIM);
        }
        // controls
        float by = py0 + ph0 + 8, bx = cx;
        bx += pill(g, "Place Here", bx, by, 0, false, mx, my, () -> { var p = minecraft.player; if (p != null) { ab.origin = p.blockPosition().relative(p.getDirection(), 2); say("Origin " + ab.origin.toShortString()); } }) + 4;
        bx += pill(g, "Rotate", bx, by, 0, false, mx, my, () -> { ab.rotation = ab.rotation.getRotated(net.minecraft.world.level.block.Rotation.CLOCKWISE_90); ab.retransform(); }) + 4;
        bx += pill(g, "Mirror", bx, by, 0, false, mx, my, () -> { var M = net.minecraft.world.level.block.Mirror.values(); ab.mirror = M[(ab.mirror.ordinal() + 1) % M.length]; ab.retransform(); }) + 4;
        chip(g, "Preview in world", bx, by + 1.5f, ab.showWhileOff && ab.preview.get(), mx, my, () -> { boolean on = !(ab.showWhileOff && ab.preview.get()); ab.showWhileOff = on; ab.preview.value = on; ConfigIO.save(); });
        float sy2 = by + 22;
        float half = (cw - 4) / 2;
        pill(g, "Start", cx, sy2, half, true, mx, my, () -> { if (ab.active == null) say("Load a schematic first"); else if (!ab.isEnabled()) ab.toggle(); });
        pill(g, "Stop", cx + half + 4, sy2, half, false, mx, my, () -> { if (ab.isEnabled()) ab.toggle(); });

        // ---------------- materials + progress
        float mx0 = cx + cw + 18, mw = wx + ww - 14 - mx0;
        card(g, mx0 - 6, top - 6, mw + 12, h + 6);
        var tot = ab.totals();
        header(g, "Materials", tot.size() + " types", mx0, top, mw);
        int[] pr = ab.progress();
        float pby = top + h - 30;
        String pt = pr[1] == 0 ? "-" : pr[0] + " / " + pr[1] + "  (" + (pr[0] * 100 / Math.max(1, pr[1])) + "%)";
        R2D.text(g, "Progress", mx0, pby, 0xFFA9A9AF);
        R2D.text(g, pt, mx0 + mw - R2D.width(pt), pby, Theme.MUTED);
        R2D.rect(g, mx0, pby + 13, mw, 5, 2.5f, Theme.TRACK);
        if (pr[1] > 0 && pr[0] > 0) R2D.rect(g, mx0, pby + 13, Math.max(5, mw * pr[0] / pr[1]), 5, 2.5f, Theme.accent());
        String org = "Origin  " + (ab.origin == null ? "not set" : ab.origin.toShortString()) + "   " + ab.rotation.name().toLowerCase().replace("_", " ").replace("none", "0°") + (ab.mirror.name().equals("NONE") ? "" : " · mirrored");
        R2D.text(g, SettingsView.fit(org, mw), mx0, pby - 14, Theme.DIM);

        float mt = top + 14, mb = pby - 20;
        R2D.pushScissor(g, mx0 - 4, mt, mw + 8, mb - mt);
        float iy = mt - matScroll;
        var inv = minecraft.player == null ? null : minecraft.player.getInventory();
        var left = ab.origin != null ? ab.materials() : tot;
        for (var e : tot.entrySet()) {
            Item it = e.getKey();
            int need = left.getOrDefault(it, 0);
            int have = inv == null ? 0 : inv.countItem(it);
            if (iy + 18 > mt && iy < mb) {
                g.pose().pushPose();
                g.pose().translate(mx0, iy + 1, 0);
                g.pose().scale(0.75f, 0.75f, 1);
                g.renderItem(new ItemStack(it), 0, 0);
                g.pose().popPose();
                R2D.text(g, SettingsView.fit(it.getDescription().getString(), mw - 70), mx0 + 17, iy + 4, 0xFFC9C9CE);
                String c = have + " / " + need;
                R2D.text(g, c, mx0 + mw - R2D.width(c), iy + 4, need == 0 ? Theme.DIM : have >= need ? Theme.GREEN : Theme.RED);
            }
            iy += 16;
        }
        if (tot.isEmpty()) R2D.text(g, "Nothing loaded", mx0, mt + 2, Theme.DIM);
        matMax = Math.max(0, iy + matScroll - mb);
        R2D.popScissor(g);

        if (toast != null && System.currentTimeMillis() - toastAt < 3500) {
            float tw = R2D.width(toast) + 16;
            R2D.rect(g, wx + (ww - tw) / 2, wy + wh - 4 - 8, tw, 14, 7, Theme.RAISED);
            R2D.text(g, toast, wx + (ww - tw) / 2 + 8, wy + wh - 8.5f, Theme.TEXT);
        }
    }

    private static String ext(String f) { int i = f.lastIndexOf('.'); return i < 0 ? "" : f.substring(i + 1); }

    private static String pretty(String id) {
        StringBuilder b = new StringBuilder();
        for (String w : id.split("_")) if (!w.isEmpty()) b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return b.toString().trim();
    }

    private float row(GuiGraphics g, String key, String label, String tag, AutoBuild ab, float x, float y, float w, float mx, float my, float lt, float lb) {
        boolean sel = key.equals(ab.loadedKey);
        boolean hov = mx >= x - 4 && mx < x + w + 4 && my >= y && my < y + 16 && my >= lt && my < lb;
        if (sel || hov) R2D.rect(g, x - 4, y, w + 8, 16, 5, sel ? Theme.accent() : Theme.RAISED);
        float tw = R2D.width(tag) + 8;
        R2D.text(g, SettingsView.fit(label, w - tw - 8), x + 2, y + 4.5f, sel ? Theme.onAccent() : hov ? Theme.TEXT : 0xFFC9C9CE);
        R2D.rect(g, x + w - tw, y + 3, tw, 10, 5, sel ? Col.alpha(0xFF000000, 0.2f) : Theme.TRACK);
        R2D.text(g, tag, x + w - tw + 4, y + 4.3f, sel ? Theme.onAccent() : Theme.DIM);
        if (y + 16 > lt && y < lb) {
            float by = Math.max(y, lt);
            btns.add(new Btn(x - 4, by, w + 8, Math.min(y + 16, lb) - by, () -> say(AutoBuild.get().load(key))));
        }
        return y + 17;
    }

    // ------------------------------------------------------------------ isometric preview

    private void iso(GuiGraphics g, Schematic sch, float x, float y, float w, float h) {
        if (sch.blocks.isEmpty()) return;
        double cyw = Math.cos(Math.toRadians(yaw)), syw = Math.sin(Math.toRadians(yaw));
        double cp = Math.cos(Math.toRadians(pitch)), sp = Math.sin(Math.toRadians(pitch));
        double ox = sch.sx / 2.0, oy = sch.sy / 2.0, oz = sch.sz / 2.0;
        double diag = Math.sqrt(sch.sx * sch.sx + sch.sy * sch.sy + sch.sz * sch.sz);
        float scale = (float) (Math.min(w, h) * 0.86 / Math.max(1, diag) * zoom);
        float cx = x + w / 2, cy = y + h / 2;
        Set<BlockPos> occ = sch.blocks.keySet();
        // faces: normal and 4 corners (unit cube)
        int[][] N = {{0, 1, 0}, {0, -1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
        double[][][] F = {
                {{0, 1, 0}, {1, 1, 0}, {1, 1, 1}, {0, 1, 1}}, {{0, 0, 0}, {0, 0, 1}, {1, 0, 1}, {1, 0, 0}},
                {{1, 0, 0}, {1, 0, 1}, {1, 1, 1}, {1, 1, 0}}, {{0, 0, 0}, {0, 1, 0}, {0, 1, 1}, {0, 0, 1}},
                {{0, 0, 1}, {0, 1, 1}, {1, 1, 1}, {1, 0, 1}}, {{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}}};
        float[] shade = {1f, 0.5f, 0.8f, 0.8f, 0.65f, 0.65f};
        boolean[] vis = new boolean[6];
        for (int i = 0; i < 6; i++) {
            double rx = N[i][0] * cyw - N[i][2] * syw, rz = N[i][0] * syw + N[i][2] * cyw;
            double z2 = N[i][1] * sp + rz * cp;
            vis[i] = z2 > 1e-4;
        }
        List<Object[]> list = new ArrayList<>();
        int cap = 0;
        for (var e : sch.blocks.entrySet()) {
            BlockPos p = e.getKey();
            BlockState s = e.getValue();
            if (s.isAir()) continue;
            boolean any = false;
            for (int i = 0; i < 6; i++) if (vis[i] && !occ.contains(p.offset(N[i][0], N[i][1], N[i][2]))) { any = true; break; }
            if (!any) continue;
            double bx = p.getX() + 0.5 - ox, by = p.getY() + 0.5 - oy, bz = p.getZ() + 0.5 - oz;
            double rz = bx * syw + bz * cyw;
            double depth = by * sp + rz * cp;
            list.add(new Object[]{depth, p, s});
            if (++cap > 60000) break;
        }
        list.sort(Comparator.comparingDouble(o -> (double) o[0]));
        R2D.pushScissor(g, x, y, w, h);
        g.flush();
        Matrix4f m = g.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (Object[] o : list) {
            BlockPos p = (BlockPos) o[1];
            BlockState s = (BlockState) o[2];
            int col = s.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col;
            if (col == 0) col = 0x8F8F8F;
            for (int i = 0; i < 6; i++) {
                if (!vis[i] || occ.contains(p.offset(N[i][0], N[i][1], N[i][2]))) continue;
                int r = (int) (((col >> 16) & 255) * shade[i]), gg = (int) (((col >> 8) & 255) * shade[i]), bl = (int) ((col & 255) * shade[i]);
                for (double[] c : F[i]) {
                    double vx = p.getX() + c[0] - ox, vy = p.getY() + c[1] - oy, vz = p.getZ() + c[2] - oz;
                    double rx = vx * cyw - vz * syw, rz = vx * syw + vz * cyw;
                    double ry = vy * cp - rz * sp;
                    b.vertex(m, cx + (float) rx * scale, cy - (float) ry * scale, 0).color(r, gg, bl, 255).endVertex();
                }
            }
        }
        Tesselator.getInstance().end();
        RenderSystem.enableCull();
        R2D.popScissor(g);
    }

    @Override
    protected boolean clickedAt(float mx, float my, int b) {
        if (mx >= px0 && mx < px0 + pw0 && my >= py0 && my < py0 + ph0) { rotating = true; lastX = mx; lastY = my; return true; }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int b, double dx, double dy) {
        float mx = (float) mouseX / s, my = (float) mouseY / s;
        if (rotating) {
            yaw += (mx - lastX) * 1.2f;
            pitch = Math.max(-5, Math.min(89, pitch + (my - lastY) * 1.0f));
            lastX = mx; lastY = my;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, b, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int b) {
        rotating = false;
        return super.mouseReleased(mx, my, b);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double d) {
        float mx = (float) mouseX / s, my = (float) mouseY / s;
        if (mx >= px0 && mx < px0 + pw0 && my >= py0 && my < py0 + ph0) zoom = Math.max(0.3f, Math.min(4f, zoom * (d > 0 ? 1.12f : 1 / 1.12f)));
        else if (mx < px0) listScroll = Math.max(0, Math.min(listMax, listScroll - (float) d * 20));
        else matScroll = Math.max(0, Math.min(matMax, matScroll - (float) d * 20));
        return true;
    }

    /** Native file picker (runs off the render thread), copies the file into .minecraft/schematics. */
    private void addSchematic() {
        Thread t = new Thread(() -> {
            String picked;
            try (MemoryStack st = MemoryStack.stackPush()) {
                PointerBuffer f = st.mallocPointer(4);
                for (String p : new String[]{"*.litematic", "*.schem", "*.schematic", "*.nbt"}) f.put(st.UTF8(p));
                f.flip();
                picked = TinyFileDialogs.tinyfd_openFileDialog("Add schematic", System.getProperty("user.home") + java.io.File.separator, f, "Schematics (.litematic .schem .nbt)", false);
            } catch (Throwable ex) {
                minecraft.execute(() -> say("File picker unavailable: " + ex.getMessage()));
                return;
            }
            if (picked == null) return;
            try {
                Path src = Path.of(picked);
                Path dst = AutoBuild.folder().resolve(src.getFileName().toString());
                Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                String name = dst.getFileName().toString();
                minecraft.execute(() -> { files = AutoBuild.files(); say(AutoBuild.get().load(name)); });
            } catch (Exception ex) {
                minecraft.execute(() -> say("Copy failed: " + ex.getMessage()));
            }
        }, "Nebula-FilePicker");
        t.setDaemon(true);
        t.start();
    }
}
