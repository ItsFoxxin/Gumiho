package dev.gumiho.client.module.utility;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ProjectilePathsModule extends GumihoModule {
    private List<Path> paths = List.of();
    private Object cachedLevel;
    private ByteBufferBuilder storage;
    private MultiBufferSource.BufferSource buffers;
    private static final int STEP_BUDGET = 1200;

    public ProjectilePathsModule() {
        super("projectile_paths", "Projectile Paths", "투사체 궤적",
                "Color-coded flight predictions and impact markers; right-click for options",
                "비행 경로와 충돌 지점을 예측합니다. 우클릭으로 설정합니다",
                ModuleCategory.UTILITY, false, false);
    }
    @Override public void tick(Minecraft client) {
        if (client.level == null || client.player == null) { suspend(client); return; }
        var c = GumihoClient.CONFIG.config();
        cachedLevel = client.level;
        double range = c.projectilePathRange;
        List<Projectile> nearby = client.level.getEntitiesOfClass(Projectile.class,
                client.player.getBoundingBox().inflate(range), p -> kind(p) != null && !p.isRemoved()
                        && client.player.distanceToSqr(p) <= range * range
                        && (p.getOwner() == client.player ? c.projectilePathsOwn : c.projectilePathsOthers));
        nearby.sort(Comparator.comparingDouble(client.player::distanceToSqr));
        List<Path> next = new ArrayList<>();
        int[] budget = {STEP_BUDGET};
        if (c.projectileHeldPreview) for (var launch : HeldProjectilePreview.read(client)) {
            if (next.size() >= (int)c.projectilePathLimit) break;
            Path preview = predictShot(client, null, launch.owner(), launch.start(), launch.velocity(),
                    launch.kind(), false, 0, (int)c.projectilePathTicks, range, budget);
            if (preview.points.size() > 1) next.add(preview);
        }
        for (Projectile projectile : nearby) {
            if (next.size() >= (int)c.projectilePathLimit || budget[0] <= 0) break;
            if (!client.player.hasLineOfSight(projectile) || stationary(projectile)) continue;
            Path path = predict(client, projectile, kind(projectile), (int)c.projectilePathTicks, range, budget);
            if (path.points.size() > 1) next.add(path);
        }
        paths = List.copyOf(next);
    }
    private static boolean stationary(Projectile p) {
        if (p instanceof AbstractArrow arrow && arrow.isNoPhysics()) return true;
        return p.tickCount > 1 && p.position().distanceToSqr(p.getPosition(0)) < 1e-10
                && (p instanceof AbstractArrow || p.getDeltaMovement().lengthSqr() < 1e-8);
    }
    public static TrajectoryPhysics.Kind kind(Projectile p) {
        var type = p.getType();
        if (p instanceof FireworkRocketEntity rocket && rocket.isShotAtAngle()) return TrajectoryPhysics.Kind.FIREWORK;
        if (type == EntityType.ARROW || type == EntityType.SPECTRAL_ARROW) return TrajectoryPhysics.Kind.ARROW;
        if (type == EntityType.TRIDENT) return TrajectoryPhysics.Kind.TRIDENT;
        if (type == EntityType.POTION) return TrajectoryPhysics.Kind.POTION;
        if (type == EntityType.EXPERIENCE_BOTTLE) return TrajectoryPhysics.Kind.EXPERIENCE;
        if (type == EntityType.ENDER_PEARL || type == EntityType.SNOWBALL || type == EntityType.EGG)
            return TrajectoryPhysics.Kind.THROWABLE;
        if (type == EntityType.WIND_CHARGE || type == EntityType.BREEZE_WIND_CHARGE) return TrajectoryPhysics.Kind.WIND;
        if (type == EntityType.WITHER_SKULL && p instanceof WitherSkull skull)
            return skull.isDangerous() ? TrajectoryPhysics.Kind.DANGEROUS_SKULL : TrajectoryPhysics.Kind.FIREBALL;
        if (type == EntityType.FIREBALL || type == EntityType.SMALL_FIREBALL || type == EntityType.DRAGON_FIREBALL)
            return TrajectoryPhysics.Kind.FIREBALL;
        return null; // Homing bullets, vertical/boost fireworks, fishing and mod-specific physics need their own models.
    }
    private static Path predict(Minecraft client, Projectile projectile, TrajectoryPhysics.Kind kind,
                                int ticks, double range, int[] budget) {
        return predictShot(client, projectile, projectile.getOwner(), projectile.position(), projectile.getDeltaMovement(),
                kind, projectile.isNoGravity(), projectile instanceof AbstractHurtingProjectile hurting ? hurting.accelerationPower : 0,
                ticks, range, budget);
    }
    private static Path predictShot(Minecraft client, Projectile projectile, Entity owner, Vec3 start, Vec3 motion,
                                    TrajectoryPhysics.Kind kind, boolean noGravity, double accel, int ticks, double range, int[] budget) {
        var state = new TrajectoryPhysics.State(start.x, start.y, start.z, motion.x, motion.y, motion.z);
        List<Vec3> points = new ArrayList<>();
        points.add(start);
        Impact impact = Impact.NONE;
        boolean leftOwner = owner == null || !owner.getBoundingBox().inflate(1).contains(start);
        for (int tick = 0; tick < ticks && budget[0]-- > 0; tick++) {
            Vec3 pos = new Vec3(state.x(), state.y(), state.z());
            if (!loaded(client, pos)) break;
            boolean water = client.level.getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER);
            var next = TrajectoryPhysics.step(state, kind, water, noGravity, accel);
            if (next == null) break;
            Vec3 end = new Vec3(next.x(), next.y(), next.z());
            if (end.distanceToSqr(client.player.getEyePosition()) > range * range
                    || pos.distanceToSqr(end) > 1024 || !loadedSegment(client, pos, end)) break;
            var blockState = client.level.getBlockState(BlockPos.containing(end));
            if (blockState.is(Blocks.NETHER_PORTAL) || blockState.is(Blocks.END_PORTAL) || blockState.is(Blocks.END_GATEWAY)) break;
            var blockHit = client.level.clip(new ClipContext(pos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile == null ? owner : projectile));
            Vec3 stop = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
            Impact nextImpact = blockHit.getType() == HitResult.Type.MISS ? Impact.NONE : Impact.BLOCK;
            double closest = pos.distanceToSqr(stop);
            AABB sweep = new AABB(pos, end).inflate(1);
            for (Entity target : client.level.getEntities(projectile == null ? owner : projectile, sweep,
                    e -> !e.isSpectator() && e.canBeHitByProjectile() && !e.noPhysics)) {
                if (!leftOwner && owner != null && target.getRootVehicle() == owner.getRootVehicle()) continue;
                if (kind == TrajectoryPhysics.Kind.WIND && (target instanceof AbstractWindCharge || target.getType() == EntityType.END_CRYSTAL)) continue;
                // Stop at the first predicted contact, including possible piercing/deflection contacts.
                AABB hitBox = target.getBoundingBox().inflate(0.3);
                var hit = hitBox.clip(pos, stop);
                if (hitBox.contains(pos) || hit.isPresent()) {
                    Vec3 point = hitBox.contains(pos) ? pos : hit.get();
                    double distance = pos.distanceToSqr(point);
                    if (distance <= closest) { stop = point; closest = distance; nextImpact = Impact.ENTITY; }
                }
            }
            points.add(stop);
            if (nextImpact != Impact.NONE) { impact = nextImpact; break; }
            state = next;
            if (!leftOwner && owner != null && !owner.getBoundingBox().inflate(1).contains(end)) leftOwner = true;
        }
        return new Path(projectile, List.copyOf(points), kind.color, impact);
    }
    private static boolean loaded(Minecraft client, Vec3 pos) {
        return Double.isFinite(pos.x) && Double.isFinite(pos.y) && Double.isFinite(pos.z)
                && pos.y >= client.level.getMinBuildHeight() && pos.y < client.level.getMaxBuildHeight() + 32
                && client.level.hasChunkAt(BlockPos.containing(pos));
    }
    private static boolean loadedSegment(Minecraft client, Vec3 start, Vec3 end) {
        int samples = Math.max(1, (int)Math.ceil(start.distanceTo(end) / 4));
        for (int i = 1; i <= samples; i++) if (!loaded(client, start.lerp(end, (double)i / samples))) return false;
        return true;
    }
    public void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !enabled()) return;
        Minecraft client = Minecraft.getInstance();
        if (!runtimeReady(client)) return;
        if (client.level == null || client.player == null || cachedLevel != client.level || client.options.hideGui || paths.isEmpty()) return;
        if (storage == null) { storage = new ByteBufferBuilder(32768); buffers = MultiBufferSource.immediate(storage); }
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        try {
            // AFTER_LEVEL has restored the global model-view; apply the event view exactly once.
            pose.mulPose(event.getModelViewMatrix());
            Vec3 camera = event.getCamera().getPosition();
            var vertices = buffers.getBuffer(ProjectilePathRenderType.PATHS);
            for (Path path : paths) {
                if (path.projectile == null && client.screen != null) continue;
                if (path.projectile != null && (path.projectile.isRemoved() || path.projectile.level() != client.level)) continue;
                Vec3 first = path.projectile == null ? path.points.get(0)
                        : path.projectile.getPosition(event.getPartialTick().getGameTimeDeltaPartialTick(false));
                line(pose, vertices, camera, first, path.points.get(0), path.color);
                for (int i = 1; i < path.points.size(); i++) line(pose, vertices, camera, path.points.get(i - 1), path.points.get(i), path.color);
                if (GumihoClient.CONFIG.config().projectileImpactMarkers && path.impact != Impact.NONE) {
                    Vec3 point = path.points.get(path.points.size() - 1);
                    int color = path.impact == Impact.ENTITY ? 0xFFFF6464 : path.color;
                    line(pose, vertices, camera, point.add(-0.16, 0, 0), point.add(0.16, 0, 0), color);
                    line(pose, vertices, camera, point.add(0, -0.16, 0), point.add(0, 0.16, 0), color);
                    line(pose, vertices, camera, point.add(0, 0, -0.16), point.add(0, 0, 0.16), color);
                }
            }
            buffers.endBatch(ProjectilePathRenderType.PATHS);
        } finally { pose.popPose(); }
    }
    private static void line(PoseStack pose, VertexConsumer vertices, Vec3 camera, Vec3 a, Vec3 b, int color) {
        Vec3 normal = b.subtract(a).normalize();
        if (normal.lengthSqr() < 1e-10) return;
        vertices.addVertex(pose.last(), (float)(a.x - camera.x), (float)(a.y - camera.y), (float)(a.z - camera.z)).setColor(color)
                .setNormal(pose.last(), (float)normal.x, (float)normal.y, (float)normal.z);
        vertices.addVertex(pose.last(), (float)(b.x - camera.x), (float)(b.y - camera.y), (float)(b.z - camera.z)).setColor(color)
                .setNormal(pose.last(), (float)normal.x, (float)normal.y, (float)normal.z);
    }
    @Override public void suspend(Minecraft client) { paths = List.of(); cachedLevel = null; }
    @Override protected void onDisable(Minecraft client) {
        suspend(client);
        if (storage != null) { storage.close(); storage = null; buffers = null; }
    }
    private enum Impact { NONE, BLOCK, ENTITY }
    private record Path(Projectile projectile, List<Vec3> points, int color, Impact impact) { }
}
