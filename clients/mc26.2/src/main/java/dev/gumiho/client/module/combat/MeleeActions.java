package dev.gumiho.client.module.combat;

import dev.gumiho.client.GumihoClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

public final class MeleeActions {
    private static final AttackGate GATE = new AttackGate();
    private static final EquipmentSnapshot EQUIPMENT = new EquipmentSnapshot();
    private MeleeActions() { }

    public static void beginTick(Minecraft client) {
        if (client.player != null) EQUIPMENT.capture(client.player, client.player.tickCount,
                client.player.getInventory().getSelectedSlot(), client.player.getMainHandItem().getItem());
    }

    public static boolean active(Minecraft client) {
        return active(client, false);
    }

    private static boolean active(Minecraft client, boolean allowOwnedShield) {
        return client.player != null && client.level != null && client.gameMode != null
                && client.player.isAlive() && !client.player.isSpectator()
                && client.gui.screen() == null && client.isWindowActive() && !client.isPaused()
                && (!client.player.isUsingItem() || allowOwnedShield && GumihoClient.AUTO_SHIELD.ownsShield(client))
                && GumihoClient.AUTO_SHIELD.allowsAutoAttack()
                // Offhand ranged aim must not compete with a main-hand melee controller.
                && !((GumihoClient.MODULES.isEnabled("bow_aim_assist") || GumihoClient.MODULES.isEnabled("player_bow_aim"))
                    && RangedWeaponState.read(client).aimActive(client))
                && !client.player.getMainHandItem().is(Items.WIND_CHARGE)
                && GumihoClient.SERVER_POLICY.canUseExperimentalModules(client);
    }

    public static boolean weapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.getItem() instanceof AxeItem
                || stack.getItem() instanceof MaceItem || stack.is(Items.TRIDENT) || SpearSupport.enabled(stack);
    }

    public static double reach(Minecraft client) {
        if (SpearSupport.enabled(client.player.getMainHandItem())) return SpearSupport.maxReach(client);
        return Math.min(GumihoClient.CONFIG.config().meleeRange,
                Math.min(GumihoClient.CONFIG.config().targetRange, client.player.entityInteractionRange()));
    }

    public static LivingEntity crosshairTarget(Minecraft client) {
        return active(client) ? lookTarget(client) : null;
    }

    /** Read-only target check also usable while holding an automatically raised shield. */
    public static LivingEntity lookTarget(Minecraft client) {
        if (client.player == null || client.level == null) return null;
        if (SpearSupport.isSpear(client.player.getMainHandItem())) return SpearSupport.safeTarget(client);
        Vec3 eye = client.player.getEyePosition();
        Vec3 end = eye.add(client.player.getLookAngle().scale(reach(client)));
        var block = client.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
        double limit = Math.min(eye.distanceToSqr(end), eye.distanceToSqr(block.getLocation()));
        // Fresh tick-time ray: a cached render hit can be stale during fast mace descents.
        // Include non-targets in the trace so a teammate/neutral entity obstructs attacks behind it.
        var hit = ProjectileUtil.getEntityHitResult(client.player, eye, end,
                client.player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),
                entity -> !entity.isSpectator() && entity.isPickable(), limit);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)
                || !CombatTargeting.eligible(client, target, CombatTargeting.TargetType.ANY)
                || !client.player.hasLineOfSight(target)) return null;
        return target;
    }

    public static boolean facingShield(Minecraft client, LivingEntity target) {
        if (!target.isBlocking()) return false;
        Vec3 towardUs = client.player.position().subtract(target.position()).multiply(1, 0, 1).normalize();
        return target.getLookAngle().multiply(1, 0, 1).normalize().dot(towardUs) > 0;
    }

    public static boolean smashReady(Minecraft client) {
        return client.player != null && MaceItem.canSmashAttack(client.player)
                && CombatRules.smashReady(client.player.fallDistance,
                client.player.getDeltaMovement().y, client.player.isFallFlying(),
                GumihoClient.CONFIG.config().maceMinimumFall);
    }

    public static boolean attack(Minecraft client, LivingEntity target, boolean requireSmash) {
        if (!active(client) || target == null || crosshairTarget(client) != target) return false;
        if (!attackRules(client, target, requireSmash)) return false;
        var player = client.player;
        ItemStack held = player.getMainHandItem();

        // Player.tick already resets charge when the held item changes. Resetting here again
        // on the first target would discard a fully recharged wind-charge-to-mace swap.
        // A different case: Auto Eat/Auto Weapon may have changed slots after Player.tick.
        if (!EQUIPMENT.unchanged(player, player.tickCount, player.getInventory().getSelectedSlot(), held.getItem())) {
            player.resetAttackStrengthTicker();
            return false;
        }
        if (!GATE.ready(player, player.tickCount, player.getAttackStrengthScale(0))) return false;
        GATE.record(player, player.tickCount);
        var piercing = held.get(net.minecraft.core.component.DataComponents.PIERCING_WEAPON);
        if (piercing != null) client.gameMode.piercingAttack(piercing);
        else client.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
        player.resetAttackStrengthTicker();
        GumihoClient.AUTO_SHIELD.attacked();
        return true;
    }

    /** Read-only probe; opening a shield window must never itself attack or reset charge. */
    public static boolean readyShieldAttack(Minecraft client) {
        if (!active(client, true)) return false;
        var player = client.player;
        ItemStack held = player.getMainHandItem();
        boolean smash = held.getItem() instanceof MaceItem && GumihoClient.MODULES.isEnabled("mace_smash");
        if (!smash && !GumihoClient.MODULES.isEnabled("cooldown_attack")) return false;
        var target = lookTarget(client);
        return target != null && attackRules(client, target, smash)
                && EQUIPMENT.unchanged(player, player.tickCount, player.getInventory().getSelectedSlot(), held.getItem())
                && GATE.ready(player, player.tickCount, player.getAttackStrengthScale(0));
    }

    private static boolean attackRules(Minecraft client, LivingEntity target, boolean requireSmash) {
        var config = GumihoClient.CONFIG.config();
        ItemStack held = client.player.getMainHandItem();
        return (!held.has(net.minecraft.core.component.DataComponents.PIERCING_WEAPON) || SpearSupport.ready(client))
                && !client.player.cannotAttackWithItem(held, 0)
                && (!config.meleeWeaponsOnly || weapon(held))
                && (!requireSmash || held.getItem() instanceof MaceItem && smashReady(client))
                && (!config.meleeWaitHurt || target.hurtTime <= 0)
                && (!config.meleeAvoidShields || !facingShield(client, target) || held.getItem() instanceof AxeItem);
    }
}
