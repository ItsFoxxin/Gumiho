package dev.gumiho.client.module.combat;

import dev.gumiho.client.gui.HudTheme;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.gui.HudEditorScreen;
import dev.gumiho.client.module.HudModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import java.util.Locale;

public final class TargetHudModule extends HudModule {
    public TargetHudModule() {
        super("target_hud", "Target / Intel HUD", "대상 정보 HUD",
                "Shared target, health, equipment, shield state, and hurt animation; compact or detailed",
                "대상, 체력, 장비, 방패 상태와 피격 동작을 간단 또는 상세 표시합니다",
                ModuleCategory.COMBAT, true, 178, 84);
    }
    @Override public void renderHud(GuiGraphicsExtractor g, Minecraft client) {
        var locked = GumihoClient.MACE_COMBO_AIM.trackedTarget(client);
        var target = locked != null ? locked : CombatTargeting.findNearestVisible(client, GumihoClient.CONFIG.config().targetRange,
                180, CombatTargeting.TargetType.ANY);
        if (target == null && GumihoClient.MODULES.isEnabled("contextual_hud")
                && !(client.gui.screen() instanceof HudEditorScreen)) return;
        boolean detailed = GumihoClient.CONFIG.config().targetHudDetailed;
        g.fill(x(), y(), x() + 210, y() + hudHeight(), HudTheme.PANEL_BACKGROUND);
        g.fill(x(), y(), x() + 3, y() + hudHeight(), 0xFFFF7A18);
        line(g, client, target == null ? "Target | 대상: None" :
                target.getName().getString() + String.format(Locale.ROOT, "  %.0f/%.0f HP",
                        target.getHealth(), target.getMaxHealth()), 4, 0xFFFFE8C2);
        String intent = target == null ? "--" : target instanceof AbstractClientPlayer player
                ? CombatAwareness.intent(player) : "VISIBLE";
        if (target != null && target.isBlocking()) {
            intent = MeleeActions.facingShield(client, target) ? "SHIELD FRONT / AXE" : "SHIELD AWAY";
        }
        if (locked != null) intent = "MACE LOCK";
        String distance = target == null ? "No eligible visible target | 대상 없음"
                : String.format(Locale.ROOT, "%.1fm • %s • %s", client.player.distanceTo(target),
                        CombatAwareness.direction(client, target.position()), intent);
        line(g, client, distance, 15, 0xFFFFA45B);
        if (detailed) {
            String equipment = target == null ? "Equipment | 장비: --"
                    : "Main " + target.getMainHandItem().getHoverName().getString()
                    + " • Off " + target.getOffhandItem().getHoverName().getString();
            line(g, client, equipment, 26, 0xFFD7BDA8);
            String state = target == null ? "Hurt animation | 피격 동작: --"
                    : "Hurt ~" + target.hurtTime + "t"
                    + (target instanceof AbstractClientPlayer player
                            ? " • " + CombatAwareness.armorStatus(player) + " • T" + CombatAwareness.threatScore(client, player) : "");
            line(g, client, state, 37, 0xFFFF8A78);
        }
        float health = target == null ? 0 : Mth.clamp(target.getHealth() / Math.max(1, target.getMaxHealth()), 0, 1);
        int barY = y() + (detailed ? 47 : 27);
        g.fill(x() + 7, barY, x() + 203, barY + 2, 0xFF34271F);
        g.fill(x() + 7, barY, x() + 7 + Math.round(196 * health), barY + 2, 0xFFE34B32);
    }
    private void line(GuiGraphicsExtractor g, Minecraft client, String text, int offset, int color) {
        String clipped = client.font.width(text) <= 196 ? text
                : client.font.plainSubstrByWidth(text, 196 - client.font.width("…")) + "…";
        g.text(client.font, clipped, x() + 7, y() + offset, color, false);
    }
    @Override public int hudWidth(Minecraft client) { return 210; }
    @Override public int hudHeight() { return GumihoClient.CONFIG.config().targetHudDetailed ? 50 : 32; }
}
