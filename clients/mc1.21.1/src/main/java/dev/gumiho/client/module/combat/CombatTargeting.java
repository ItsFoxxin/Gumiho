package dev.gumiho.client.module.combat;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.config.GumihoConfig;
import dev.gumiho.client.tracking.PlayerGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public final class CombatTargeting {
    private static Object cachedLevel;
    private static int cachedTick;
    private static List<LivingEntity> candidates = List.of();
    private CombatTargeting() { }

    private static List<LivingEntity> candidates(Minecraft client) {
        if (cachedLevel != client.level || cachedTick != client.player.tickCount) {
            cachedLevel = client.level;
            cachedTick = client.player.tickCount;
            List<LivingEntity> loaded = new ArrayList<>();
            for (Entity entity : client.level.entitiesForRendering()) {
                if (entity instanceof LivingEntity living) loaded.add(living);
            }
            candidates = loaded;
        }
        return candidates;
    }

    public static boolean eligible(Minecraft client, LivingEntity living, TargetType type) {
        if (client.player == null || client.level == null || living == client.player
                || living.level() != client.level || !living.isAlive() || living.isRemoved()
                || living.isSpectator() || !living.isAttackable() || living instanceof ArmorStand) return false;
        GumihoConfig config = GumihoClient.CONFIG.config();
        if (living.isInvisible() && !config.targetInvisible) return false;
        CombatRules.Kind kind;
        if (living instanceof Player player) {
            if (player.isCreative() || type == TargetType.MOB) return false;
            PlayerGroup group = GumihoClient.PLAYER_GROUPS.groupOf(player.getScoreboardName());
            kind = switch (group) {
                case ENEMY -> CombatRules.Kind.ENEMY_PLAYER;
                case TEAMMATE -> CombatRules.Kind.TEAMMATE;
                case NEUTRAL -> CombatRules.Kind.NEUTRAL_PLAYER;
            };
        } else {
            if (type == TargetType.PLAYER) return false;
            kind = living instanceof Enemy ? CombatRules.Kind.HOSTILE
                    : living instanceof Mob ? CombatRules.Kind.PASSIVE : CombatRules.Kind.INVALID;
        }
        return CombatRules.allowed(kind, config.targetHostileMobs, config.targetEnemyPlayers, config.targetPassiveMobs);
    }

    public static LivingEntity findNearestVisible(Minecraft client, double maxDistance, double maxAngle, TargetType type) {
        if (client.player == null || client.level == null) return null;
        GumihoConfig config = GumihoClient.CONFIG.config();
        double range = Math.min(maxDistance, config.targetRange);
        double halfFov = Math.min(maxAngle, config.targetFov / 2.0D);
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        Vec3 eye = client.player.getEyePosition();
        Vec3 look = client.player.getLookAngle().normalize();
        for (LivingEntity living : candidates(client)) {
            if (!eligible(client, living, type)) continue;
            double distance = eye.distanceTo(living.getBoundingBox().getCenter());
            if (distance > range || !client.player.hasLineOfSight(living)) continue;
            Vec3 direction = living.getBoundingBox().getCenter().subtract(eye).normalize();
            double angle = Math.toDegrees(Math.acos(Mth.clamp(look.dot(direction), -1.0D, 1.0D)));
            if (angle > halfFov) continue;
            boolean player = living instanceof Player;
            double score = CombatRules.score(config.targetPriority, angle, distance, living.getHealth(),
                    player && GumihoClient.PLAYER_GROUPS.isPinned(living.getScoreboardName()),
                    player && GumihoClient.MODULES.isEnabled("prioritize_weak_enemies"));
            if (score < bestScore) { bestScore = score; best = living; }
        }
        return best;
    }

    public enum TargetType { MOB, PLAYER, ANY }
}
