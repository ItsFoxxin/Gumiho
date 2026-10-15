package dev.gumiho.client.module.combat;

import com.mojang.blaze3d.vertex.*;
import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import dev.gumiho.client.module.utility.ProjectilePathRenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

public final class MaceComboAimModule extends GumihoModule {
    private final StableTarget<LivingEntity> lock = new StableTarget<>();
    private final MaceFlight flight = new MaceFlight();
    private long lastFrame;
    private Object session;
    private Vec3 landing, desired;
    private boolean landedPrediction;
    private double landingTicks;
    private ByteBufferBuilder storage;
    private MultiBufferSource.BufferSource buffers;
    public MaceComboAimModule() {
        super("mace_combo_lock", "Mace Combo Assist", "철퇴 연속 보조",
                "Reacquire reachable bounce targets; landing guide and optional normal-input air correction",
                "도약마다 가능한 대상을 재선택하고 착지 안내와 선택적 공중 이동 보조를 제공합니다",
                ModuleCategory.COMBAT, false, true);
    }
    private boolean active(Minecraft c) {
        return enabled() && runtimeReady(c) && MeleeActions.active(c) && c.player.getMainHandItem().getItem() instanceof MaceItem;
    }
    private static boolean normalFlight(Minecraft c) {
        return !c.player.getAbilities().flying && !c.player.isFallFlying() && !c.player.isInWaterOrBubble()
                && !c.player.isInLava() && !c.player.isPassenger() && !c.player.hasEffect(MobEffects.SLOW_FALLING)
                && !c.player.hasEffect(MobEffects.LEVITATION);
    }
    private double range() { return Math.min(GumihoClient.CONFIG.config().maceLockRange, GumihoClient.CONFIG.config().targetRange); }
    public LivingEntity trackedTarget(Minecraft c) { return active(c) && Double.isFinite(score(c, lock.current())) ? lock.current() : null; }
    private MaceFlight.Landing projection(Minecraft c, LivingEntity target) {
        Vec3 pos = c.player.position(), v = c.player.getDeltaMovement();
        return MaceFlight.atHeight(pos.x, pos.y, pos.z, v.x, v.y, v.z, target.getY());
    }
    private double score(Minecraft c, LivingEntity target) {
        if (target == null || !CombatTargeting.eligible(c, target, CombatTargeting.TargetType.ANY)
                || !c.player.hasLineOfSight(target)) return Double.POSITIVE_INFINITY;
        Vec3 delta = target.getBoundingBox().getCenter().subtract(c.player.getEyePosition());
        if (delta.length() > range()) return Double.POSITIVE_INFINITY;
        boolean ground = c.player.onGround() || !normalFlight(c);
        if (ground) {
            if (c.player.getLookAngle().dot(delta.normalize()) < Math.cos(Math.toRadians(Math.min(180, GumihoClient.CONFIG.config().targetFov)/2)))
                return Double.POSITIVE_INFINITY;
            return MaceFlight.score(0, 0, 0, true, delta.length(), MeleeActions.reach(c));
        }
        var predicted = projection(c, target);
        if (predicted == null) return Double.POSITIVE_INFINITY;
        Vec3 targetPoint = target.position().add(target.getKnownMovement().multiply(1,0,1).scale(Math.min(predicted.ticks(), 10)));
        double error = Math.hypot(predicted.x()-targetPoint.x, predicted.z()-targetPoint.z);
        return MaceFlight.score(error, predicted.ticks(), MaceFlight.correctionRadius(predicted.ticks(), GumihoClient.CONFIG.config().maceResetRadius), false, delta.length(), MeleeActions.reach(c));
    }
    @Override public void tick(Minecraft c) {
        if (!active(c)) { suspend(c); return; }
        if (session != c.player) { suspend(c); session = c.player; }
        if (flight.newCycle(c.player.onGround(), c.player.getDeltaMovement().y)) lock.reset();
        LivingEntity best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        boolean pinned = false;
        for (var target : c.level.getEntitiesOfClass(LivingEntity.class, c.player.getBoundingBox().inflate(range()))) {
            double score = score(c, target);
            if (!Double.isFinite(score)) continue;
            boolean pin = target instanceof Player && GumihoClient.PLAYER_GROUPS.isPinned(target.getScoreboardName());
            if (score < bestScore && !pinned || pin && !pinned) { best = target; bestScore = score; pinned = pin; }
        }
        double currentScore = score(c, lock.current());
        // A substantially better landing window or a new bounce can replace an otherwise visible old target.
        lock.update(best, Double.isFinite(currentScore), pinned || bestScore + 3 < currentScore);
        updateLanding(c);
        desired = null;
        if (lock.current() != null && Double.isFinite(score(c, lock.current()))) {
            var t = lock.current();
            var p = projection(c, t);
            desired = t.position().add(t.getKnownMovement().multiply(1,0,1).scale(p == null ? 0 : Math.min(p.ticks(), 10)));
        }
    }
    private void updateLanding(Minecraft c) {
        landing = null; landedPrediction = false; landingTicks = 0;
        if (c.player.onGround() || !normalFlight(c)) return;
        Vec3 pos = c.player.position(), v = c.player.getDeltaMovement();
        for (int tick = 0; tick < 60; tick++) {
            Vec3 end = pos.add(v);
            if (!c.level.hasChunkAt(BlockPos.containing(end))) return;
            var hit = c.level.clip(new ClipContext(pos.add(0,0.03,0), end.add(0,0.03,0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, c.player));
            if (hit.getType() != HitResult.Type.MISS) {
                if (hit.getDirection() == Direction.UP) {
                    landing = hit.getLocation().add(0,0.04,0); landedPrediction = true;
                    landingTicks = tick + pos.distanceTo(hit.getLocation()) / Math.max(0.01, v.length());
                }
                return;
            }
            pos = end; v = new Vec3(v.x * 0.91, (v.y - 0.08) * 0.98, v.z * 0.91);
        }
    }
    public String landingHint(Minecraft c) {
        if (!active(c)) return "Combo Assist OFF | 연속 보조 꺼짐";
        if (!normalFlight(c)) return "Flight mode: manual | 특수 비행";
        if (c.player.onGround()) return "Ground: near targets only | 지상";
        if (landing == null) return "Landing unknown | 착지 미확인";
        if (desired == null) return String.format(java.util.Locale.ROOT, "Land ~%.1fs | No reset target", landingTicks/20);
        double gap = Math.hypot(landing.x-desired.x, landing.z-desired.z);
        var move = MaceFlight.steering(desired.x-landing.x, desired.z-landing.z, c.player.getYRot());
        String direction = gap < 0.7 ? "ALIGNED" : Math.abs(move.forward()) >= Math.abs(move.left())
                ? move.forward() > 0 ? "FORWARD" : "BACK" : move.left() > 0 ? "LEFT" : "RIGHT";
        return String.format(java.util.Locale.ROOT, "%s %.1fm | Land ~%.1fs", direction, gap, landingTicks/20);
    }
    public void onInput(MovementInputUpdateEvent event) {
        Minecraft c = Minecraft.getInstance();
        if (event.getEntity() != c.player || !GumihoClient.CONFIG.config().maceAirControl || !active(c)
                || c.player.onGround() || !normalFlight(c) || landing == null || desired == null
                || !Double.isFinite(score(c, lock.current()))) return;
        var input = event.getInput();
        // Manual steering/jump/crouch always wins; never write velocity or position.
        if (input.up || input.down || input.left || input.right || input.jumping || input.shiftKeyDown
                || input.forwardImpulse != 0 || input.leftImpulse != 0) return;
        var steering = MaceFlight.steering(desired.x-landing.x, desired.z-landing.z, c.player.getYRot());
        input.forwardImpulse = steering.forward(); input.leftImpulse = steering.left();
    }
    @Override public void frame(Minecraft c, float partialTick) {
        if (!active(c) || !Double.isFinite(score(c, lock.current()))) { lastFrame = 0; return; }
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : (now-lastFrame)/1e9; lastFrame = now;
        var target = lock.current();
        var center = target.getPosition(partialTick).add(target.getBoundingBox().getCenter().subtract(target.position()));
        if (!c.player.onGround()) center = center.add(target.getKnownMovement().scale(0.5));
        var delta = center.subtract(c.player.getEyePosition(partialTick));
        double yaw = Math.toDegrees(Math.atan2(delta.z, delta.x))-90;
        double pitch = -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
        var config = GumihoClient.CONFIG.config();
        if (Math.hypot(delta.x, delta.z) > 0.2)
            c.player.setYRot(c.player.getYRot()+(float)AimSmoothing.step(Mth.wrapDegrees(yaw-c.player.getYRot()),dt,config.meleeAimSpeed*20,config.meleeAimResponse));
        c.player.setXRot(Mth.clamp(c.player.getXRot()+(float)AimSmoothing.step(pitch-c.player.getXRot(),dt,config.meleeAimSpeed*20,config.meleeAimResponse),-90,90));
    }
    public void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft c = Minecraft.getInstance();
        if (!active(c) || !GumihoClient.CONFIG.config().maceLandingGuide || c.options.hideGui || !landedPrediction || landing == null) return;
        if (storage == null) { storage = new ByteBufferBuilder(4096); buffers = MultiBufferSource.immediate(storage); }
        var pose = event.getPoseStack(); pose.pushPose();
        try {
            pose.mulPose(event.getModelViewMatrix());
            var vertices = buffers.getBuffer(ProjectilePathRenderType.PATHS);
            Vec3 camera = event.getCamera().getPosition();
            ring(pose, vertices, camera, landing, 0xFFFFB04A);
            if (desired != null) {
                Vec3 destination = desired.add(0,0.06,0);
                ring(pose, vertices, camera, destination, 0xFF82E68A);
                line(pose,vertices,camera,landing,destination,0xFFFFE8C2);
            }
            buffers.endBatch(ProjectilePathRenderType.PATHS);
        } finally { pose.popPose(); }
    }
    private static void ring(PoseStack pose, VertexConsumer v, Vec3 camera, Vec3 center, int color) {
        for (int i=0;i<24;i++) {
            double a=i*Math.PI/12,b=(i+1)*Math.PI/12;
            line(pose,v,camera,center.add(Math.cos(a)*0.45,0,Math.sin(a)*0.45),
                    center.add(Math.cos(b)*0.45,0,Math.sin(b)*0.45),color);
        }
    }
    private static void line(PoseStack pose, VertexConsumer v, Vec3 camera, Vec3 a, Vec3 b, int color) {
        Vec3 n=b.subtract(a).normalize(), ra=a.subtract(camera), rb=b.subtract(camera);
        if(n.lengthSqr()<1e-10)return;
        v.addVertex(pose.last(),(float)ra.x,(float)ra.y,(float)ra.z).setColor(color).setNormal(pose.last(),(float)n.x,(float)n.y,(float)n.z);
        v.addVertex(pose.last(),(float)rb.x,(float)rb.y,(float)rb.z).setColor(color).setNormal(pose.last(),(float)n.x,(float)n.y,(float)n.z);
    }
    @Override public void suspend(Minecraft c) { lock.reset(); flight.reset(); session=null; lastFrame=0; landing=null; desired=null; landedPrediction=false; }
    @Override protected void onDisable(Minecraft c) {
        suspend(c); if(storage!=null){storage.close();storage=null;buffers=null;}
    }
}
