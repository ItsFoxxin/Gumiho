package dev.gumiho.client.module.combat;

import dev.gumiho.client.gui.HudTheme;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.gui.HudEditorScreen;
import dev.gumiho.client.module.HudModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Items;

/** Retains the saved HUD position/toggle ID; one panel replaces the old bow-only panel. */
public final class BowStatusHudModule extends HudModule {
    private static final int WIDTH = 188;
    private String title = "Ranged | 원거리";
    private String detail = "Hold bow / crossbow / trident";
    private float charge;
    private boolean visible;
    private Object level;

    public BowStatusHudModule() {
        super("bow_status_hud", "Ranged Status HUD", "원거리 상태 HUD",
                "Weapon charge, loaded crossbow and trident readiness; right-click for settings",
                "무기 충전과 쇠뇌, 삼지창 상태를 표시합니다. 우클릭으로 설정합니다",
                ModuleCategory.COMBAT, true, 6, 90);
    }
    @Override public void tick(Minecraft client) {
        level = client.level;
        var state = RangedWeaponState.read(client);
        visible = state.kind() != RangedWeaponRules.Kind.NONE;
        charge = state.charge();
        title = switch (state.kind()) {
            case BOW -> "Bow | 활";
            case CROSSBOW -> "Crossbow | 쇠뇌";
            case TRIDENT -> "Trident | 삼지창";
            default -> "Ranged | 원거리";
        };
        int arrows = 0;
        if (client.player != null) for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
            var stack = client.player.getInventory().getItem(i);
            if (stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW) || stack.is(Items.TIPPED_ARROW)) arrows += stack.getCount();
        }
        detail = switch (state.kind()) {
            case BOW -> !state.ammo() ? "No ammo | 화살 없음"
                    : (charge >= 1 ? "FULL | 완충" : "Draw | 당기기") + "  Arrows: " + arrows;
            case CROSSBOW -> state.special() ? "Firework | 폭죽: manual aim"
                    : state.loaded() ? "LOADED | 장전  Hold aim key"
                    : !state.ammo() ? "No ammo | 화살 없음"
                    : charge >= 1 ? "Release to load | 장전 완료"
                    : "Loading | 장전 중  " + arrows + " arrows";
            case TRIDENT -> !state.ammo() ? "Too damaged | 내구도 부족"
                    : state.special() ? "Riptide | 급류: manual aim"
                    : charge >= 1 ? "READY | 준비: release to throw"
                    : "Charge | 충전: hold Use";
            default -> "Hold bow / crossbow / trident";
        };
    }
    @Override public void renderHud(GuiGraphics g, Minecraft client) {
        boolean editor = client.screen instanceof HudEditorScreen;
        if (client.player == null && !editor) return;
        if ((!visible || level != client.level) && GumihoClient.MODULES.isEnabled("contextual_hud") && !editor) return;
        g.fill(x(), y(), x() + WIDTH, y() + hudHeight(), HudTheme.PANEL_BACKGROUND);
        g.fill(x(), y(), x() + 3, y() + hudHeight(), 0xFFFF7A18);
        String percent = Math.round(charge * 100) + "%";
        int percentX = x() + WIDTH - 6 - client.font.width(percent);
        g.drawString(client.font, client.font.plainSubstrByWidth(title, percentX - x() - 14), x() + 7, y() + 4, 0xFFFFE8C2, false);
        g.drawString(client.font, percent, percentX, y() + 4, 0xFFFFE8C2, false);
        g.drawString(client.font, client.font.plainSubstrByWidth(detail, WIDTH - 14), x() + 7, y() + 16, 0xFFFFA45B, false);
        g.fill(x() + 7, y() + 29, x() + WIDTH - 6, y() + 33, 0xFF34271F);
        g.fill(x() + 7, y() + 29, x() + 7 + Math.round((WIDTH - 13) * charge), y() + 33, 0xFFFF7A18);
    }
    @Override public int hudWidth(Minecraft client) { return WIDTH; }
    @Override public int hudHeight() { return 38; }
}
