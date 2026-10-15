package dev.gumiho.client.module.combat;

import dev.gumiho.client.gui.HudTheme;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.gui.HudEditorScreen;
import dev.gumiho.client.module.HudModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.MaceItem;
import java.util.Locale;

public final class MaceStatusHudModule extends HudModule {
    public MaceStatusHudModule() {
        super("mace_status_hud", "Mace Status", "철퇴 상태",
                "Fall distance, smash readiness, and unenchanted bonus estimate; not a landing guarantee",
                "낙하 거리와 강타 준비 상태, 기본 추가 피해 예상값을 표시합니다",
                ModuleCategory.COMBAT, true, 6, 52);
    }
    @Override public void renderHud(GuiGraphics g, Minecraft client) {
        boolean held = client.player != null && client.player.getMainHandItem().getItem() instanceof MaceItem;
        if (!held && !(client.screen instanceof HudEditorScreen)) return;
        double fall = client.player == null ? 0 : client.player.fallDistance;
        boolean ready = held && MeleeActions.smashReady(client);
        boolean charged = client.player != null && client.player.getAttackStrengthScale(0) >= 1;
        String state = ready ? (charged ? "SMASH READY" : "RECHARGING") : "NOT READY";
        g.fill(x(), y(), x() + hudWidth(client), y() + hudHeight(), HudTheme.PANEL_BACKGROUND);
        g.fill(x(), y(), x() + 3, y() + hudHeight(), 0xFFFF7A18);
        g.drawString(client.font, "Mace | 철퇴: " + state, x() + 7, y() + 4,
                ready && charged ? 0xFF82E68A : 0xFFFFE8C2, false);
        String detail = String.format(Locale.ROOT, "Fall %.1fm • Base bonus ~+%.1f HP", fall,
                ready ? CombatRules.baseSmashBonus(fall) : 0);
        g.drawString(client.font, client.font.plainSubstrByWidth(detail, 162), x() + 7, y() + 17, 0xFFFFA45B, false);
        g.drawString(client.font, client.font.plainSubstrByWidth(GumihoClient.MACE_COMBO_AIM.landingHint(client), 162), x()+7, y()+30, 0xFF82E68A, false);
    }
    @Override public int hudWidth(Minecraft client) { return 176; }
    @Override public int hudHeight() { return 45; }
}
