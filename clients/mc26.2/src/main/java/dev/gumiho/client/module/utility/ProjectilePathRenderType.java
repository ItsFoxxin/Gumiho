package dev.gumiho.client.module.utility;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/** Immutable camera-relative geometry crosses 26.2's extraction/submission boundary. */
public final class ProjectilePathRenderType {
    public static final ContextKey<List<Segment>> PATH_DATA = new ContextKey<>(Identifier.fromNamespaceAndPath("gumiho", "paths"));
    public static final ContextKey<List<Segment>> MACE_DATA = new ContextKey<>(Identifier.fromNamespaceAndPath("gumiho", "mace_guide"));
    public static final RenderType PATHS = RenderType.create("gumiho_paths",
            RenderSetup.builder(RenderPipelines.LINES_TRANSLUCENT).createRenderSetup());
    public record Segment(Vec3 a, Vec3 b, int color) { }

    public static void onSubmit(SubmitCustomGeometryEvent event) {
        submit(event, event.getLevelRenderState().getRenderData(PATH_DATA));
        submit(event, event.getLevelRenderState().getRenderData(MACE_DATA));
    }
    private static void submit(SubmitCustomGeometryEvent event, List<Segment> lines) {
        if (lines == null || lines.isEmpty()) return;
        event.getSubmitNodeCollector().submitCustomGeometry(event.getPoseStack(), PATHS, (pose, vertices) -> {
            for (Segment line : lines) {
                Vec3 n = line.b.subtract(line.a).normalize();
                if (n.lengthSqr() < 1e-10) continue;
                vertex(pose, vertices, line.a, n, line.color);
                vertex(pose, vertices, line.b, n, line.color);
            }
        });
    }
    private static void vertex(PoseStack.Pose pose, VertexConsumer out, Vec3 p, Vec3 n, int color) {
        out.addVertex(pose, (float)p.x, (float)p.y, (float)p.z).setColor(color)
                .setNormal(pose, (float)n.x, (float)n.y, (float)n.z).setLineWidth(2);
    }
    public static void line(List<Segment> out, Vec3 camera, Vec3 a, Vec3 b, int color) {
        out.add(new Segment(a.subtract(camera), b.subtract(camera), color));
    }
    public static void ring(List<Segment> out, Vec3 camera, Vec3 center, int color) {
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 12, b = (i + 1) * Math.PI / 12;
            line(out, camera, center.add(Math.cos(a)*0.45, 0, Math.sin(a)*0.45),
                    center.add(Math.cos(b)*0.45, 0, Math.sin(b)*0.45), color);
        }
    }
    private ProjectilePathRenderType() { }
}
