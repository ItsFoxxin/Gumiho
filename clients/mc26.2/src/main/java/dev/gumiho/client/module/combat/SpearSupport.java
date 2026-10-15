package dev.gumiho.client.module.combat;

import dev.gumiho.client.GumihoClient;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;

/** Native piercing traces include every possible collateral hit, not just the first target. */
public final class SpearSupport {
    public static boolean isSpear(ItemStack stack) {
        return stack.is(ItemTags.SPEARS) && stack.has(DataComponents.PIERCING_WEAPON)
                && stack.has(DataComponents.ATTACK_RANGE);
    }
    public static boolean enabled(ItemStack stack) {
        return isSpear(stack) && GumihoClient.MODULES.isEnabled("spear_assist");
    }
    public static Collection<EntityHitResult> hits(Minecraft c) {
        return ProjectileUtil.getHitEntitiesAlong(c.player, c.player.getAttackRangeWith(c.player.getMainHandItem()),
                entity -> PiercingWeapon.canHitEntity(c.player, entity), ClipContext.Block.COLLIDER)
                .<Collection<EntityHitResult>>map(block -> List.of(), entities -> entities);
    }
    public static LivingEntity safeTarget(Minecraft c) {
        if (c.player == null || c.level == null || !enabled(c.player.getMainHandItem())) return null;
        LivingEntity nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (EntityHitResult hit : hits(c)) {
            // A server jab hits the full trace. Refuse the entire action for ANY excluded entity.
            if (!(hit.getEntity() instanceof LivingEntity living)
                    || !CombatTargeting.eligible(c, living, CombatTargeting.TargetType.ANY)
                    || !c.player.hasLineOfSight(living)) return null;
            var cfg = GumihoClient.CONFIG.config();
            if (cfg.meleeWaitHurt && living.hurtTime > 0 || cfg.meleeAvoidShields && MeleeActions.facingShield(c, living)) return null;
            double distance = c.player.getEyePosition().distanceTo(hit.getLocation());
            if (distance > GumihoClient.CONFIG.config().targetRange) return null;
            if (distance < best) { nearest = living; best = distance; }
        }
        return nearest;
    }
    public static boolean ready(Minecraft c) {
        if (!enabled(c.player.getMainHandItem())) return false;
        var stack = c.player.getMainHandItem();
        return SpearRules.safeJab(safeTarget(c) == null ? 0 : 1, 0,
                GumihoClient.CONFIG.config().spearJab, c.player.isUsingItem() && !GumihoClient.AUTO_SHIELD.ownsShield(c),
                c.player.getAttackStrengthScale(0), !c.player.cannotAttackWithItem(stack, 0)
                        && !c.player.getCooldowns().isOnCooldown(stack));
    }
    public static double maxReach(Minecraft c) {
        return Math.min(GumihoClient.CONFIG.config().targetRange,
                c.player.getAttackRangeWith(c.player.getMainHandItem()).effectiveMaxRange(c.player));
    }
    public static boolean inAimRange(Minecraft c, LivingEntity target) {
        var range = c.player.getAttackRangeWith(c.player.getMainHandItem());
        double distance = c.player.getEyePosition().distanceTo(target.getBoundingBox().getCenter());
        return SpearRules.inBand(distance, range.effectiveMinRange(c.player), maxReach(c));
    }
    private SpearSupport() { }
}
