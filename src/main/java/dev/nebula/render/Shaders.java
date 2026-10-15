package dev.nebula.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.nebula.Nebula;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;

import java.io.IOException;

public final class Shaders {
    public static ShaderInstance ROUNDED;
    public static ShaderInstance BLUR;
    public static ShaderInstance FADE;

    private Shaders() {}

    public static void onRegister(RegisterShadersEvent e) {
        try {
            e.registerShader(new ShaderInstance(e.getResourceProvider(),
                    new ResourceLocation(Nebula.MODID, "rounded"), DefaultVertexFormat.POSITION_TEX_COLOR), s -> ROUNDED = s);
            e.registerShader(new ShaderInstance(e.getResourceProvider(),
                    new ResourceLocation(Nebula.MODID, "blur"), DefaultVertexFormat.POSITION_TEX), s -> BLUR = s);
            e.registerShader(new ShaderInstance(e.getResourceProvider(),
                    new ResourceLocation(Nebula.MODID, "fade"), DefaultVertexFormat.POSITION_TEX), s -> FADE = s);
        } catch (IOException ex) {
            Nebula.LOG.error("Failed to load Nebula shaders", ex);
        }
    }
}
