package dev.gumiho.client.module.combat;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class SpearAssistModule extends GumihoModule {
    private final StableTarget<LivingEntity> lock = new StableTarget<>();
    private long lastFrame;
    public SpearAssistModule() {
        super("spear_assist", "Spear Assist", "창 보조",
                "Native jab/charge aim; full-charge jabs require Cooldown Attack. Right-click for options",
                "창 찌르기와 돌진 조준. 자동 찌르기는 재사용 공격이 필요합니다",
                ModuleCategory.COMBAT, false, true);
    }
    private boolean active(Minecraft c) {
        if (!enabled() || !canRun(c, GumihoClient.SERVER_POLICY) || c.player == null || c.level == null
                || c.gameMode == null || !c.player.isAlive() || c.player.isSpectator()
                || c.gui.screen() != null || !c.isWindowActive() || c.isPaused()
                || !SpearSupport.isSpear(c.player.getMainHandItem()) || !GumihoClient.CONFIG.config().spearAim) return false;
        return !c.player.isUsingItem() || c.player.getUseItem() == c.player.getMainHandItem()
                && GumihoClient.CONFIG.config().spearChargeAim;
    }
    private double score(Minecraft c, LivingEntity target) {
        if (target == null || !CombatTargeting.eligible(c, target, CombatTargeting.TargetType.ANY)
                || !SpearSupport.inAimRange(c, target) || !c.player.hasLineOfSight(target)) return Double.POSITIVE_INFINITY;
        var delta = target.getBoundingBox().getCenter().subtract(c.player.getEyePosition());
        double angle = Math.toDegrees(Math.acos(Mth.clamp(c.player.getLookAngle().dot(delta.normalize()), -1, 1)));
        var cfg = GumihoClient.CONFIG.config();
        if (angle > Math.min(90, cfg.targetFov / 2)) return Double.POSITIVE_INFINITY;
        return CombatRules.score(cfg.targetPriority, angle, delta.length(), target.getHealth(),
                GumihoClient.PLAYER_GROUPS.isPinned(target.getScoreboardName()), false);
    }
    @Override public void tick(Minecraft c) {
        if (!active(c)) { suspend(c); return; }
        LivingEntity best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (var target : c.level.getEntitiesOfClass(LivingEntity.class, c.player.getBoundingBox().inflate(SpearSupport.maxReach(c)))) {
            double score = score(c, target);
            if (score < bestScore) { best = target; bestScore = score; }
        }
        lock.update(best, Double.isFinite(score(c, lock.current())),
                best != null && GumihoClient.PLAYER_GROUPS.isPinned(best.getScoreboardName()));
    }
    @Override public void frame(Minecraft c, float partial) {
        if (!active(c) || !Double.isFinite(score(c, lock.current()))) { lastFrame = 0; return; }
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : (now - lastFrame) / 1e9;
        lastFrame = now;
        var target = lock.current();
        var center = target.getPosition(partial).add(target.getBoundingBox().getCenter().subtract(target.position()));
        var delta = center.subtract(c.player.getEyePosition(partial));
        double yaw = Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
        double pitch = -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
        var cfg = GumihoClient.CONFIG.config();
        c.player.setYRot(c.player.getYRot() + (float)AimSmoothing.step(Mth.wrapDegrees(yaw-c.player.getYRot()), dt, cfg.meleeAimSpeed*20, cfg.meleeAimResponse));
        c.player.setXRot(Mth.clamp(c.player.getXRot() + (float)AimSmoothing.step(pitch-c.player.getXRot(), dt, cfg.meleeAimSpeed*20, cfg.meleeAimResponse), -90, 90));
    }
    @Override public void suspend(Minecraft c) { lock.reset(); lastFrame = 0; }
    @Override protected void onDisable(Minecraft c) { suspend(c); }
}
