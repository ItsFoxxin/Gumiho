package dev.gumiho.client.profile;

public enum CombatProfile {
    CUSTOM("Custom | 사용자"),
    ASSIST("Assist | 보조"),
    ENHANCED("Enhanced | 강화"),
    AGGRESSIVE("Aggressive | 공격"),
    WAR("War | 전쟁");

    private final String label;

    CombatProfile(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
