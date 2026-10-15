package dev.gumiho.client.module.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.ArrayList;

/** Creates prediction inputs only: never creates/spawns entities or consumes items. */
final class HeldProjectilePreview {
    record Launch(Vec3 start, Vec3 velocity, TrajectoryPhysics.Kind kind, Entity owner) { }
    static List<Launch> read(Minecraft c) {
        if (c.player == null || c.screen != null || c.options.hideGui) return List.of();
        var p = c.player;
        ItemStack stack = p.isUsingItem() ? p.getUseItem() : p.getMainHandItem();
        if (!supported(stack) && !p.isUsingItem()) stack = p.getOffhandItem();
        if (!supported(stack) || p.getCooldowns().isOnCooldown(stack.getItem())) return List.of();
        boolean using = p.isUsingItem() && p.getUseItem() == stack;
        double speed, offset = 0;
        boolean inherited = true;
        TrajectoryPhysics.Kind kind;
        int count = 1;
        if (stack.is(Items.BOW)) {
            if (!using || p.getProjectile(stack).isEmpty() && !p.hasInfiniteMaterials()) return List.of();
            double power = BowItem.getPowerForTime(p.getTicksUsingItem());
            if (power < 0.1) return List.of();
            speed = 3 * power; kind = TrajectoryPhysics.Kind.ARROW;
        } else if (stack.is(Items.CROSSBOW)) {
            var charged = stack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
            if (charged.isEmpty() && !using) return List.of();
            var ammo = charged.isEmpty() ? p.getProjectile(stack) : charged.getItems().get(0);
            if (ammo.isEmpty()) return List.of();
            boolean rocket = ammo.is(Items.FIREWORK_ROCKET);
            speed = rocket ? 1.6 : 3.15; kind = rocket ? TrajectoryPhysics.Kind.FIREWORK : TrajectoryPhysics.Kind.ARROW;
            inherited = false;
            count = Math.min(3, Math.max(1, charged.getItems().size()));
        } else if (stack.is(Items.TRIDENT)) {
            if (!using || EnchantmentHelper.getTridentSpinAttackStrength(stack, p) > 0
                    || stack.getDamageValue() >= stack.getMaxDamage() - 1) return List.of();
            speed = 2.5; kind = TrajectoryPhysics.Kind.TRIDENT;
        } else if (stack.is(Items.WIND_CHARGE)) {
            speed = 1.5; kind = TrajectoryPhysics.Kind.WIND;
        } else if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
            speed = 0.5; offset = -20; kind = TrajectoryPhysics.Kind.POTION;
        } else if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            speed = 0.7; offset = -20; kind = TrajectoryPhysics.Kind.EXPERIENCE;
        } else { speed = 1.5; kind = TrajectoryPhysics.Kind.THROWABLE; }
        Vec3 origin = p.getEyePosition().add(0, kind == TrajectoryPhysics.Kind.WIND ? 0 : kind == TrajectoryPhysics.Kind.FIREWORK ? -0.15 : -0.1, 0);
        Vec3 extra = inherited ? p.getKnownMovement() : Vec3.ZERO;
        if (p.onGround()) extra = new Vec3(extra.x, 0, extra.z);
        var launches = new ArrayList<Launch>();
        var raw = LaunchMath.direction(p.getXRot(), p.getYRot(), offset, speed);
        if (raw == null) return List.of();
        Vec3 central = new Vec3(raw.x(), raw.y(), raw.z());
        for (int i = 0; i < count; i++) {
            // Vanilla multishot's center and +/-10 degree side shots, rotated around camera up.
            double angle = i == 0 ? 0 : Math.toRadians(i == 1 ? -10 : 10);
            Vec3 up = p.getUpVector(1).normalize();
            Vec3 direction = central.scale(Math.cos(angle)).add(up.cross(central).scale(Math.sin(angle)))
                    .add(up.scale(up.dot(central) * (1 - Math.cos(angle))));
            launches.add(new Launch(origin, direction.add(extra), kind, p));
        }
        return launches;
    }
    private static boolean supported(ItemStack s) {
        return s.is(Items.BOW) || s.is(Items.CROSSBOW) || s.is(Items.TRIDENT) || s.is(Items.WIND_CHARGE)
                || s.is(Items.SPLASH_POTION) || s.is(Items.LINGERING_POTION) || s.is(Items.EXPERIENCE_BOTTLE)
                || s.is(Items.ENDER_PEARL) || s.is(Items.SNOWBALL) || s.is(Items.EGG);
    }
}
