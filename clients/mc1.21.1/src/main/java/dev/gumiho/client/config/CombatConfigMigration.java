package dev.gumiho.client.config;

import dev.gumiho.client.module.combat.CombatRules;
import java.util.Set;

public final class CombatConfigMigration {
    private CombatConfigMigration() { }
    public static void normalize(GumihoConfig config) {
        if (config.schemaVersion < 1) {
            Boolean intel = config.modules.remove("enemy_intel_hud");
            if (intel != null) {
                if (intel) {
                    config.modules.put("target_hud", true);
                    config.hudX.put("target_hud", config.hudX.getOrDefault("enemy_intel_hud", 178));
                    config.hudY.put("target_hud", config.hudY.getOrDefault("enemy_intel_hud", 84));
                }
                config.targetHudDetailed = intel;
                if (!intel && config.hudX.getOrDefault("target_hud", 6) == 6
                        && config.hudY.getOrDefault("target_hud", 52) == 52) {
                    config.hudX.put("target_hud", 178);
                    config.hudY.put("target_hud", 84);
                }
            }
            config.hudX.remove("enemy_intel_hud");
            config.hudY.remove("enemy_intel_hud");
            config.schemaVersion = 1;
        }
        if (config.schemaVersion < 2) {
            if (config.meleeAimSpeed == 3.0D) config.meleeAimSpeed = 6.0D;
            if (config.meleeAimResponse == 0.12D) config.meleeAimResponse = 0.08D;
            config.schemaVersion = 2;
        }
        config.bowAimRange = CombatRules.bounded(config.bowAimRange, 64, 16, 96);
        config.bowAimSmoothness = CombatRules.bounded(config.bowAimSmoothness, 3.5, 0.5, 10);
        config.rangedFov = CombatRules.bounded(config.rangedFov, 70, 10, 180);
        config.rangedAimResponse = CombatRules.bounded(config.rangedAimResponse, 0.1, 0.05, 0.4);
        config.targetRange = CombatRules.bounded(config.targetRange, 64, 4, 96);
        config.targetFov = CombatRules.bounded(config.targetFov, 120, 10, 360);
        config.meleeRange = CombatRules.bounded(config.meleeRange, 3, 1, 6);
        config.meleeAimSpeed = CombatRules.bounded(config.meleeAimSpeed, 6, 0.5, 10);
        config.meleeAimResponse = CombatRules.bounded(config.meleeAimResponse, 0.08, 0.05, 0.4);
        config.shieldRange = CombatRules.bounded(config.shieldRange, 4.5, 2, 6);
        config.shieldLeadTicks = CombatRules.bounded(config.shieldLeadTicks, 7, 5, 12);
        config.maceResetRadius = CombatRules.bounded(config.maceResetRadius, 4, 1, 8);
        config.maceLockRange = CombatRules.bounded(config.maceLockRange, 32, 6, 64);
        config.projectilePathRange = CombatRules.bounded(config.projectilePathRange, 64, 16, 128);
        config.projectilePathTicks = CombatRules.bounded(config.projectilePathTicks, 40, 10, 80);
        config.projectilePathLimit = CombatRules.bounded(config.projectilePathLimit, 12, 1, 24);
        config.shieldHoldTicks = CombatRules.bounded(config.shieldHoldTicks, 12, 6, 30);
        config.maceMinimumFall = CombatRules.bounded(config.maceMinimumFall, 1.6, 1.6, 12);
        if (config.targetPriority == null || !Set.of("CROSSHAIR", "DISTANCE", "HEALTH").contains(config.targetPriority)) {
            config.targetPriority = "CROSSHAIR";
        }
    }
}
