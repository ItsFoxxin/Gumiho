package dev.gumiho.client.module.utility;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import java.util.OptionalDouble;

/** Main-target depth-tested lines, usable after Fabulous has composited its entity target. */
public final class ProjectilePathRenderType extends RenderType {
    public static final RenderType PATHS = create("gumiho_projectile_paths", DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES, 32768, CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.of(2)))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE)
                    .setOutputState(MAIN_TARGET).setCullState(NO_CULL).createCompositeState(false));
    private ProjectilePathRenderType() {
        super("unused", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 32768,
                false, false, () -> {}, () -> {});
    }
}
