package dev.gumiho.client.module;

public enum ModuleCategory {
    COMBAT("Combat", "전투"),
    MOVEMENT("Movement", "이동"),
    VISUAL("Visual", "시각"),
    UTILITY("Utility", "유틸리티"),
    SETTINGS("Settings", "설정");

    private final String english;
    private final String korean;

    ModuleCategory(String english, String korean) {
        this.english = english;
        this.korean = korean;
    }

    public String label() {
        return english + " | " + korean;
    }
}
