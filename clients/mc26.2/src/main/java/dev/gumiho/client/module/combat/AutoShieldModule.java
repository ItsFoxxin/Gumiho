package dev.gumiho.client.module.combat;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.entity.projectile.hurtingprojectile.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import dev.gumiho.client.module.utility.ProjectilePathsModule;
import dev.gumiho.client.module.utility.TrajectoryPhysics;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.HashSet;
import java.util.Set;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

public final class AutoShieldModule extends GumihoModule {
    private final ImpactGuard guard = new ImpactGuard();
    private final ShieldAttackWindow attackWindow = new ShieldAttackWindow();
    private long clock;
    private LocalPlayer session;
    private Object levelSession;
    private Set<Integer> previousSwings = new HashSet<>();
    private LocalPlayer owner;

    public void beforeTick(ClientTickEvent.Pre event) {
        clock++;
        Minecraft client = Minecraft.getInstance();
        if (!runtimeReady(client)) return;
        if (session != client.player || levelSession != client.level || !enabled() || !canRun(client, GumihoClient.SERVER_POLICY)
                || !available(client)) suspend(client);
        session = client.player;
        levelSession = client.level;
    }

    public void onMouse(InputEvent.MouseButton.Pre event) {
        Minecraft client = Minecraft.getInstance();
        if (!runtimeReady(client)) return;
        if (event.getAction() == GLFW.GLFW_PRESS && client.options.keyAttack.getKey().getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE && client.options.keyAttack.getKey().getValue() == event.getButton())
            manualAttack(client);
    }

    public void onKey(InputEvent.Key event) {
        Minecraft client = Minecraft.getInstance();
        if (!runtimeReady(client)) return;
        if (event.getAction() == GLFW.GLFW_PRESS && client.options.keyAttack.matches(event.getKeyEvent()))
            manualAttack(client);
    }

    private void manualAttack(Minecraft client) {
        if (!enabled() || !canRun(client, GumihoClient.SERVER_POLICY) || !available(client)
                || !GumihoClient.CONFIG.config().shieldAttackWindows || UseKeyState.physical(client)) return;
        if (!ownsShield(client) && !attackWindow.open(clock)) return;
        // This runs before vanilla discards attack clicks in its using-item branch.
        // Leave the click untouched: vanilla still decides what to hit or mine.
        attackWindow.manual(clock);
        release(client, false);
    }

    public boolean ownsShield(Minecraft client) {
        return owner != null && owner == client.player && usingOffhandShield(owner);
    }

    public boolean allowsAutoAttack() { return attackWindow.allowsAutoAttack(clock); }
    public void attacked() { attackWindow.attacked(clock); }

    public AutoShieldModule() {
        super("auto_shield", "Auto Shield", "자동 방패",
                "Briefly block predicted frontal impacts; manual use/building takes priority",
                "예측된 전방 충돌을 잠시 막습니다. 수동 사용과 건설이 우선합니다",
                ModuleCategory.COMBAT, false, true);
    }
    private boolean available(Minecraft client) {
        return client.player != null && client.level != null && client.gameMode != null
                && client.player.isAlive() && !client.player.isSpectator() && client.gui.screen() == null
                && client.isWindowActive() && !client.isPaused()
                && !client.player.getMainHandItem().is(Items.WIND_CHARGE)
                && !GumihoClient.WIND_MACE_SWAP.launchGrace(client)
                && !(client.player.getMainHandItem().getItem() instanceof MaceItem
                    && !client.player.onGround() && GumihoClient.MODULES.isEnabled("mace_smash"))
                && client.player.getOffhandItem().is(Items.SHIELD)
                && !client.player.getCooldowns().isOnCooldown(client.player.getOffhandItem());
    }
    @Override public void tick(Minecraft client) {
        if (!available(client) || (owner != null && owner != client.player)) { suspend(client); return; }
        if (UseKeyState.physical(client)) { manualHandoff(client); return; }
        if (owner != null && !usingOffhandShield(client.player)) { suspend(client); return; }
        if (owner == null && (client.player.isUsingItem() || client.options.keyUse.isDown())) {
            guard.reset(); previousSwings.clear(); return;
        }
        var config = GumihoClient.CONFIG.config();
        if (!config.shieldAttackWindows) attackWindow.reset();
        double eta = config.shieldProjectiles ? incomingProjectileTicks(client) : Double.POSITIVE_INFINITY;
        if (config.shieldExplosions) eta = Math.min(eta, explosionTicks(client));
        boolean swing = config.shieldMeleeSwings && newMeleeSwing(client);
        boolean closeThreat = config.shieldCloseGuard && closeMeleeThreat(client);
        if (!config.shieldMeleeSwings) previousSwings.clear();
        boolean raise = guard.update(client.player.tickCount, eta, (int)Math.round(config.shieldLeadTicks),
                swing || closeThreat);
        // A lost target or cancelled hit cannot leave defense down indefinitely.
        if (attackWindow.open(clock)) { release(client, false); return; }
        if (config.shieldAttackWindows && ownsShield(client)
                && attackWindow.automatic(clock, MeleeActions.readyShieldAttack(client), eta)) {
            release(client, false);
            return;
        }
        if (!raise) { release(client, false); return; }
        if (owner == null) {
            client.gameMode.useItem(client.player, InteractionHand.OFF_HAND);
            if (!usingOffhandShield(client.player)) { guard.reset(); return; }
            owner = client.player;
            attackWindow.raised(clock);
        }
        client.options.keyUse.setDown(true);
    }
    @Override public void frame(Minecraft client, float partialTick) {
        if (owner == null) return;
        if (!available(client) || owner != client.player) suspend(client);
        else if (UseKeyState.physical(client)) manualHandoff(client);
    }
    private static boolean usingOffhandShield(LocalPlayer player) {
        return player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND
                && player.getUseItem().is(Items.SHIELD);
    }
    private void release(Minecraft client, boolean manualHandoff) {
        if (owner == null) return;
        boolean physical = UseKeyState.physical(client);
        // End only OUR use before allowing the physical click to place blocks/use mainhand.
        if (owner == client.player && (manualHandoff || !physical) && client.gameMode != null && usingOffhandShield(owner))
            client.gameMode.releaseUsingItem(owner);
        client.options.keyUse.setDown(physical);
        owner = null;
    }
    private void manualHandoff(Minecraft client) {
        release(client, true); guard.reset(); attackWindow.reset(); previousSwings.clear();
    }
    @Override public void suspend(Minecraft client) {
        release(client, false); guard.reset(); attackWindow.reset(); previousSwings.clear();
    }
    @Override protected void onDisable(Minecraft client) { suspend(client); }
    private static boolean inFront(Minecraft client, Vec3 position) {
        // Native shield facing is horizontal head yaw, even when looking straight up/down.
        return ShieldThreatRules.frontal(Vec3.directionFromRotation(0, client.player.getYHeadRot())
                .dot(position.subtract(client.player.position()).multiply(1, 0, 1).normalize()));
    }
    private boolean newMeleeSwing(Minecraft client) {
        Set<Integer> now = new HashSet<>();
        boolean fresh = false;
        double range = Math.min(3.5, GumihoClient.CONFIG.config().shieldRange);
        for (var target : client.level.getEntitiesOfClass(LivingEntity.class, client.player.getBoundingBox().inflate(range))) {
            if (!target.swinging || target == client.player || !target.isAlive() || target.isSpectator()
                    || client.player.distanceToSqr(target) > range * range || !inFront(client, target.position())
                    || !client.player.hasLineOfSight(target)) continue;
            Vec3 towardUs = client.player.position().subtract(target.position()).multiply(1, 0, 1).normalize();
            if (target.getLookAngle().multiply(1, 0, 1).normalize().dot(towardUs) <= 0.3) continue;
            now.add(target.getId());
            if (!previousSwings.contains(target.getId())) fresh = true;
        }
        previousSwings = now;
        return fresh;
    }
    private static boolean closeMeleeThreat(Minecraft client) {
        double range = Math.min(3.5, GumihoClient.CONFIG.config().shieldRange);
        for (var target : client.level.getEntitiesOfClass(LivingEntity.class, client.player.getBoundingBox().inflate(range))) {
            if (target == client.player || !target.isAlive() || target.isSpectator()
                    || !(target instanceof Player || target instanceof Enemy || target instanceof Mob mob && mob.isAggressive())
                    || target instanceof Creeper || !inFront(client, target.position()) || !client.player.hasLineOfSight(target)) continue;
            Vec3 toward = client.player.position().subtract(target.position()).multiply(1, 0, 1).normalize();
            boolean facing = target.getLookAngle().multiply(1, 0, 1).normalize().dot(toward) > 0.3;
            double distance = target.distanceTo(client.player);
            if (ShieldThreatRules.closeThreat(distance, range, true, true, facing ? 1 : 0)) return true;
        }
        return false;
    }
    private static double explosionTicks(Minecraft client) {
        double eta = Double.POSITIVE_INFINITY;
        for (var entity : client.level.getEntities(client.player, client.player.getBoundingBox().inflate(13))) {
            if (!inFront(client, entity.position()) || !client.player.hasLineOfSight(entity)) continue;
            if (entity instanceof PrimedTnt tnt && client.player.distanceToSqr(tnt) <= 64) eta = Math.min(eta, tnt.getFuse());
            if (entity instanceof Creeper creeper && creeper.getSwellDir() > 0 && client.player.distanceToSqr(creeper) <= (creeper.isPowered() ? 144 : 36))
                eta = Math.min(eta, Math.max(0, (1 - creeper.getSwelling(1)) * 30));
        }
        return eta;
    }
    private static double incomingProjectileTicks(Minecraft client) {
        double earliest = Double.POSITIVE_INFINITY;
        for (var arrow : client.level.getEntitiesOfClass(Projectile.class, client.player.getBoundingBox().inflate(32))) {
            boolean blockable = ShieldThreatRules.projectile(arrow instanceof AbstractArrow,
                    arrow instanceof AbstractArrow a ? a.getPierceLevel() : 0,
                    arrow instanceof AbstractArrow a && a.isNoPhysics(),
                    arrow instanceof SmallFireball || arrow instanceof LargeFireball || arrow instanceof WitherSkull || arrow instanceof ShulkerBullet
                            || arrow instanceof FireworkRocketEntity rocket && rocket.isShotAtAngle());
            if (!blockable || arrow.getOwner() == client.player || !inFront(client, arrow.position())) continue;
            var kind = arrow instanceof ShulkerBullet ? TrajectoryPhysics.Kind.WIND : ProjectilePathsModule.kind(arrow);
            if (kind == null) continue;
            // Embedded arrows retain velocity data but no longer move.
            if (arrow.tickCount > 1 && arrow.position().distanceToSqr(arrow.getPosition(0)) < 1e-8) continue;
            Vec3 velocity = arrow.getDeltaMovement();
            if (velocity.lengthSqr() < 0.01) continue;
            Vec3 pos = arrow.position();
            for (int tick = 0; tick < 18 && tick < earliest; tick++) {
                Vec3 next = pos.add(velocity);
                var box = client.player.getBoundingBox().inflate(0.35)
                        .move(client.player.getDeltaMovement().multiply(1, 0, 1).scale(Math.min(tick, 6)));
                var impact = box.clip(pos, next);
                Vec3 end = box.contains(pos) ? pos : impact.orElse(next);
                if (client.level.clip(new ClipContext(pos, end, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, arrow)).getType() != HitResult.Type.MISS) break;
                if (box.contains(pos) || impact.isPresent()) {
                    double fraction = velocity.length() > 0 ? pos.distanceTo(end) / velocity.length() : 0;
                    earliest = Math.min(earliest, tick + fraction);
                    break;
                }
                boolean water = client.level.getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER);
                var step = TrajectoryPhysics.step(new TrajectoryPhysics.State(pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z),
                        kind, water, arrow.isNoGravity(), arrow instanceof AbstractHurtingProjectile h ? h.accelerationPower : 0);
                if (step == null) break;
                pos = next;
                velocity = new Vec3(step.vx(), step.vy(), step.vz());
            }
        }
        return earliest;
    }
}
