package dev.gumiho.client.module.combat;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.MaceItem;

public final class MeleeAimModule extends GumihoModule {
    private final StableTarget<LivingEntity> lock = new StableTarget<>();
    private long lastFrame;

    public MeleeAimModule() {
        super("melee_aim", "Melee Aim", "근접 조준",
                "Eased, stable visible aim with a sword, axe, mace or trident; does not attack",
                "검, 도끼, 철퇴, 삼지창으로 안정적으로 조준합니다. 공격은 별도입니다",
                ModuleCategory.COMBAT, false, true);
    }

    private boolean active(Minecraft client) {
        return MeleeActions.active(client) && MeleeActions.weapon(client.player.getMainHandItem())
                && !(client.player.getMainHandItem().getItem() instanceof MaceItem
                    && GumihoClient.MODULES.isEnabled("mace_combo_lock"));
    }

    private boolean valid(Minecraft client, LivingEntity target) {
        if (target == null || !CombatTargeting.eligible(client, target, CombatTargeting.TargetType.ANY)) return false;
        var delta = target.getBoundingBox().getCenter().subtract(client.player.getEyePosition());
        double halfFov = Math.min(90, GumihoClient.CONFIG.config().targetFov / 2);
        return delta.length() <= MeleeActions.reach(client) && client.player.hasLineOfSight(target)
                && client.player.getLookAngle().dot(delta.normalize()) >= Math.cos(Math.toRadians(halfFov));
    }

    @Override public void tick(Minecraft client) {
        if (!active(client)) { suspend(client); return; }
        var best = CombatTargeting.findNearestVisible(client, MeleeActions.reach(client), 90, CombatTargeting.TargetType.ANY);
        lock.update(best, valid(client, lock.current()), best instanceof Player
                && GumihoClient.PLAYER_GROUPS.isPinned(best.getScoreboardName()));
    }

    @Override public void frame(Minecraft client, float partialTick) {
        if (!active(client) || !valid(client, lock.current())) { suspend(client); return; }
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : (now - lastFrame) / 1_000_000_000.0;
        lastFrame = now;
        var target = lock.current();
        // Interpolate movement instead of chasing 20 Hz position jumps.
        var center = target.getPosition(partialTick).add(target.getBoundingBox().getCenter().subtract(target.position()));
        var delta = center.subtract(client.player.getEyePosition(partialTick));
        double yaw = Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
        double pitch = -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
        var config = GumihoClient.CONFIG.config();
        double speed = config.meleeAimSpeed * 20;
        client.player.setYRot(client.player.getYRot() + (float) AimSmoothing.step(
                Mth.wrapDegrees(yaw - client.player.getYRot()), dt, speed, config.meleeAimResponse));
        client.player.setXRot(Mth.clamp(client.player.getXRot() + (float) AimSmoothing.step(
                pitch - client.player.getXRot(), dt, speed, config.meleeAimResponse), -90, 90));
    }

    @Override public void suspend(Minecraft client) { lock.reset(); lastFrame = 0; }
    @Override protected void onDisable(Minecraft client) { suspend(client); }
}
