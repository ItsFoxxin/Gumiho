package dev.gumiho.client.module.combat;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.module.combat.RangedWeaponRules.Kind;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Read-only native weapon state. Never fires, swaps slots or writes use-key state. */
public record RangedWeaponState(ItemStack stack, Kind kind, float charge, double speed,
                                boolean using, boolean loaded, boolean special, boolean ammo) {
    public static RangedWeaponState read(Minecraft client) {
        if (client.player == null) return none();
        var p = client.player;
        ItemStack stack = p.isUsingItem() ? p.getUseItem() : p.getMainHandItem();
        if (kind(stack) == Kind.NONE && !p.isUsingItem()) stack = p.getOffhandItem();
        Kind kind = kind(stack);
        boolean using = p.isUsingItem() && p.getUseItem() == stack;
        int ticks = using ? p.getTicksUsingItem() : 0;
        if (kind == Kind.BOW) {
            float charge = using ? BowItem.getPowerForTime(ticks) : 0;
            return new RangedWeaponState(stack, kind, charge, 3 * charge, using, false, false,
                    !p.getProjectile(stack).isEmpty() || p.hasInfiniteMaterials());
        }
        if (kind == Kind.CROSSBOW) {
            var loaded = stack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
            boolean charged = !loaded.isEmpty();
            var ammo = charged ? loaded.items().get(0).create() : p.getProjectile(stack);
            boolean rocket = charged ? loaded.contains(Items.FIREWORK_ROCKET) : ammo.is(Items.FIREWORK_ROCKET);
            float charge = charged ? 1 : Math.min(1, (float)ticks / Math.max(1, CrossbowItem.getChargeDuration(stack, p)));
            return new RangedWeaponState(stack, kind, charge, 3.15, using, charged, rocket, charged || !ammo.isEmpty());
        }
        if (kind == Kind.TRIDENT) {
            boolean riptide = EnchantmentHelper.getTridentSpinAttackStrength(stack, p) > 0;
            boolean usable = stack.getDamageValue() < stack.getMaxDamage() - 1;
            return new RangedWeaponState(stack, kind, Math.min(1, ticks / 10F), 2.5, using, false, riptide, usable);
        }
        return none();
    }
    public boolean aimActive(Minecraft client) {
        var c = GumihoClient.CONFIG.config();
        boolean enabled = switch (kind) {
            case BOW -> c.rangedBows;
            case CROSSBOW -> c.rangedCrossbows;
            case TRIDENT -> c.rangedTridents;
            default -> false;
        };
        return RangedWeaponRules.canAim(kind, enabled, using, loaded, GumihoClient.RANGED_AIM_KEY.isDown(),
                charge, special, ammo, client.player.getCooldowns().isOnCooldown(stack));
    }
    private static Kind kind(ItemStack s) {
        return s.is(Items.BOW) ? Kind.BOW : s.is(Items.CROSSBOW) ? Kind.CROSSBOW : s.is(Items.TRIDENT) ? Kind.TRIDENT : Kind.NONE;
    }
    private static RangedWeaponState none() {
        return new RangedWeaponState(ItemStack.EMPTY, Kind.NONE, 0, 0, false, false, false, false);
    }
}
