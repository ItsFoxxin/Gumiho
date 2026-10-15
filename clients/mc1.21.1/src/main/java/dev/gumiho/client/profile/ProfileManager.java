package dev.gumiho.client.profile;

import dev.gumiho.client.config.GumihoConfigManager;
import dev.gumiho.client.module.ModuleManager;
import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ProfileManager {
    private static final Set<String> ASSIST = Set.of(
            "brand_hud", "coordinates", "player_tracker_hud", "target_hud", "bow_status_hud",
            "attack_cooldown_hud", "survival_alerts", "projectile_warning", "mace_status_hud", "directional_indicators",
            "effects_hud", "cooldown_hud", "contextual_hud", "show_neutral_players", "prioritize_weak_enemies"
    );
    private static final Set<String> ENHANCED = plus(ASSIST,
            "auto_totem", "auto_eat", "bridge_assist", "bow_aim_assist", "auto_sprint");
    private static final Set<String> AGGRESSIVE = plus(ENHANCED,
            "player_bow_aim", "cooldown_attack");
    private static final Set<String> WAR = plus(AGGRESSIVE,
            "fullbright", "no_weather");

    private final GumihoConfigManager configManager;
    private final ModuleManager moduleManager;

    public ProfileManager(GumihoConfigManager configManager, ModuleManager moduleManager) {
        this.configManager = configManager;
        this.moduleManager = moduleManager;
    }

    public CombatProfile current() {
        try {
            return CombatProfile.valueOf(configManager.config().activeProfile.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return CombatProfile.CUSTOM;
        }
    }

    public CombatProfile cycle(Minecraft client) {
        CombatProfile[] values = CombatProfile.values();
        CombatProfile next = values[(current().ordinal() + 1) % values.length];
        apply(next, client);
        return next;
    }

    public void apply(CombatProfile profile, Minecraft client) {
        configManager.config().activeProfile = profile.name();
        if (profile != CombatProfile.CUSTOM) {
            moduleManager.applyProfile(modulesFor(profile), client);
        } else {
            configManager.save();
        }
    }

    public void markCustom() {
        configManager.config().activeProfile = CombatProfile.CUSTOM.name();
        configManager.save();
    }

    private static Set<String> modulesFor(CombatProfile profile) {
        return switch (profile) {
            case ASSIST -> ASSIST;
            case ENHANCED -> ENHANCED;
            case AGGRESSIVE -> AGGRESSIVE;
            case WAR -> WAR;
            case CUSTOM -> Set.of();
        };
    }

    private static Set<String> plus(Set<String> base, String... additions) {
        Set<String> result = new HashSet<>(base);
        result.addAll(Set.of(additions));
        return Set.copyOf(result);
    }
}
