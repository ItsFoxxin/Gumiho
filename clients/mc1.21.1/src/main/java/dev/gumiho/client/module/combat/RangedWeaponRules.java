package dev.gumiho.client.module.combat;

/** Pure activation policy, separate from native item/enchantment reads. */
public final class RangedWeaponRules {
    private RangedWeaponRules() { }
    public enum Kind { NONE, BOW, CROSSBOW, TRIDENT }
    public static boolean canAim(Kind kind, boolean enabled, boolean using, boolean loaded, boolean holdAim,
                                 double charge, boolean special, boolean ammo, boolean cooldown) {
        if (kind == null || !enabled || special || !ammo || cooldown || !Double.isFinite(charge)) return false;
        return switch (kind) {
            case BOW -> using && charge >= 0.1;
            case CROSSBOW -> using || loaded && holdAim;
            case TRIDENT -> using && charge >= 1;
            default -> false;
        };
    }
}
