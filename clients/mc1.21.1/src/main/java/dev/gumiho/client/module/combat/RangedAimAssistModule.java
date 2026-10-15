package dev.gumiho.client.module.combat;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import dev.gumiho.client.module.utility.TrajectoryPhysics;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

/** Shared implementation behind the independently selectable mob/player cards. */
public abstract class RangedAimAssistModule extends GumihoModule {
    private final boolean playerModule;
    private final StableTarget<LivingEntity> lock = new StableTarget<>();
    private long lastFrame;
    private Vec3 aimVelocity;
    private int solvedTick;
    private RangedWeaponRules.Kind solvedWeapon;
    protected RangedAimAssistModule(boolean playerModule) {
        super(playerModule ? "player_bow_aim" : "bow_aim_assist",
                playerModule ? "Player Ranged Aim" : "Mob Ranged Aim",
                playerModule ? "플레이어 원거리 조준" : "몹 원거리 조준",
                "Stable bow/crossbow/trident aim; manual fire. Right-click for ranged settings",
                "활, 쇠뇌, 삼지창 조준 보조. 발사는 수동이며 우클릭으로 설정합니다",
                ModuleCategory.COMBAT, false, true);
        this.playerModule = playerModule;
    }
    private boolean active(Minecraft c) {
        return c.player != null && c.level != null && c.player.isAlive() && !c.player.isSpectator()
                && c.screen == null && c.isWindowActive() && !c.isPaused()
                && GumihoClient.SERVER_POLICY.canUseExperimentalModules(c)
                && RangedBallistics.drives(playerModule, GumihoClient.MODULES.isEnabled("bow_aim_assist"),
                    GumihoClient.MODULES.isEnabled("player_bow_aim"))
                && RangedWeaponState.read(c).aimActive(c);
    }
    private CombatTargeting.TargetType type() {
        boolean mobs = GumihoClient.MODULES.isEnabled("bow_aim_assist");
        boolean players = GumihoClient.MODULES.isEnabled("player_bow_aim");
        return mobs && players ? CombatTargeting.TargetType.ANY
                : players ? CombatTargeting.TargetType.PLAYER : CombatTargeting.TargetType.MOB;
    }
    private boolean valid(Minecraft c, LivingEntity target) {
        if (target == null || !CombatTargeting.eligible(c, target, type()) || !c.player.hasLineOfSight(target)) return false;
        var config = GumihoClient.CONFIG.config();
        Vec3 delta = target.getBoundingBox().getCenter().subtract(c.player.getEyePosition());
        return delta.length() <= Math.min(config.bowAimRange, config.targetRange)
                && c.player.getLookAngle().dot(delta.normalize()) >= Math.cos(Math.toRadians(Math.min(config.rangedFov, config.targetFov) / 2));
    }
    @Override public void tick(Minecraft c) {
        if (!active(c)) { suspend(c); return; }
        var config = GumihoClient.CONFIG.config();
        var best = CombatTargeting.findNearestVisible(c, config.bowAimRange, config.rangedFov / 2, type());
        lock.update(best, valid(c, lock.current()), best instanceof Player && GumihoClient.PLAYER_GROUPS.isPinned(best.getScoreboardName()));
        aimVelocity = null;
        if (!valid(c, lock.current())) { lastFrame = 0; return; }
        var weapon = RangedWeaponState.read(c);
        Vec3 start = c.player.getEyePosition().add(0, -0.1, 0);
        Vec3 inherited = weapon.kind() == RangedWeaponRules.Kind.CROSSBOW ? Vec3.ZERO : c.player.getKnownMovement();
        if (c.player.onGround()) inherited = new Vec3(inherited.x, 0, inherited.z);
        var target = lock.current();
        Vec3 velocity = config.rangedLead ? target.getKnownMovement() : Vec3.ZERO;
        if (target.onGround()) velocity = new Vec3(velocity.x, 0, velocity.z);
        var solution = RangedBallistics.solve(vector(target.getBoundingBox().getCenter().subtract(start)),
                vector(velocity), vector(inherited), weapon.speed());
        if (solution == null) { lastFrame = 0; return; }
        Vec3 launch = new Vec3(solution.launch().x(), solution.launch().y(), solution.launch().z());
        if (config.rangedCheckPath && !clearPath(c, target, start, launch.add(inherited), solution.ticks())) {
            lastFrame = 0; return;
        }
        aimVelocity = launch;
        solvedTick = c.player.tickCount;
        solvedWeapon = weapon.kind();
    }
    private static RangedBallistics.Vector vector(Vec3 v) { return new RangedBallistics.Vector(v.x, v.y, v.z); }
    private static boolean clearPath(Minecraft c, LivingEntity target, Vec3 start, Vec3 velocity, double ticks) {
        var state = new TrajectoryPhysics.State(start.x, start.y, start.z, velocity.x, velocity.y, velocity.z);
        for (int i = 0; i < Math.ceil(ticks); i++) {
            if (!c.level.hasChunkAt(BlockPos.containing(start)) || !c.level.getFluidState(BlockPos.containing(start)).isEmpty()) return false;
            var next = TrajectoryPhysics.step(state, TrajectoryPhysics.Kind.ARROW, false, false, 0);
            if (next == null) return false;
            Vec3 end = new Vec3(next.x(), next.y(), next.z());
            if (i + 1 > ticks) end = start.lerp(end, ticks - i);
            if (!c.level.hasChunkAt(BlockPos.containing(end)) || !c.level.getFluidState(BlockPos.containing(end)).isEmpty()) return false;
            var hit = c.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.player));
            if (hit.getType() != HitResult.Type.MISS) return false;
            for (var entity : c.level.getEntities(c.player, new AABB(start, end).inflate(1), e -> e.isPickable() && !e.isSpectator())) {
                if (entity == target) continue;
                var box = entity.getBoundingBox().inflate(0.3);
                if (box.contains(start) || box.clip(start, end).isPresent()) return false;
            }
            state = next; start = end;
        }
        return true;
    }
    @Override public void frame(Minecraft c, float partialTick) {
        if (!active(c) || !valid(c, lock.current()) || aimVelocity == null
                || c.player.tickCount - solvedTick > 1 || RangedWeaponState.read(c).kind() != solvedWeapon) {
            lastFrame = 0; return;
        }
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : (now - lastFrame) / 1e9;
        lastFrame = now;
        double yaw = Math.toDegrees(Math.atan2(aimVelocity.z, aimVelocity.x)) - 90;
        double pitch = -Math.toDegrees(Math.atan2(aimVelocity.y, Math.hypot(aimVelocity.x, aimVelocity.z)));
        var config = GumihoClient.CONFIG.config();
        c.player.setYRot(c.player.getYRot() + (float)AimSmoothing.step(Mth.wrapDegrees(yaw - c.player.getYRot()), dt,
                config.bowAimSmoothness * 20, config.rangedAimResponse));
        c.player.setXRot(Mth.clamp(c.player.getXRot() + (float)AimSmoothing.step(pitch - c.player.getXRot(), dt,
                config.bowAimSmoothness * 20, config.rangedAimResponse), -90, 90));
    }
    @Override public void suspend(Minecraft c) { lock.reset(); lastFrame = 0; aimVelocity = null; solvedWeapon = null; }
    @Override protected void onDisable(Minecraft c) { suspend(c); }
}
