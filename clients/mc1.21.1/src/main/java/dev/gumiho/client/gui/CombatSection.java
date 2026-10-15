package dev.gumiho.client.gui;

public enum CombatSection {
    ALL("All | 전체"), MELEE("Melee | 근접"), MACE("Mace | 철퇴"),
    RANGED("Ranged | 원거리"), SPEAR("Spears | 창"),
    DEFENSE("Defense | 방어"), AWARENESS("Awareness | 정보");
    private final String label;
    CombatSection(String label) { this.label = label; }
    public String label() { return label; }
    public static CombatSection of(String id) {
        return switch (id) {
            case "melee_aim", "cooldown_attack", "auto_weapon", "attack_cooldown_hud" -> MELEE;
            case "mace_combo_lock", "mace_smash", "mace_status_hud", "wind_mace_swap" -> MACE;
            case "bow_aim_assist", "player_bow_aim", "bow_status_hud" -> RANGED;
            case "spear_assist", "spear_status_hud" -> SPEAR;
            case "auto_shield", "auto_totem", "auto_eat" -> DEFENSE;
            default -> AWARENESS;
        };
    }
}
