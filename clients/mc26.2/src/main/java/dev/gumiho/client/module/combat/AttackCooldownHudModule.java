package dev.gumiho.client.module.combat;

import dev.gumiho.client.gui.HudTheme;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.gui.HudEditorScreen;
import dev.gumiho.client.module.HudModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.EntityHitResult;

public final class AttackCooldownHudModule extends HudModule {
    private static final int WIDTH = 154;

    public AttackCooldownHudModule() {
        super("attack_cooldown_hud", "Attack Cooldown HUD", "공격 재사용 HUD",
                "Show when your next melee hit is fully charged", "다음 근접 공격이 완전히 충전되는 시점을 표시합니다",
                ModuleCategory.COMBAT, true, 6, 134);
    }

    @Override
    public void renderHud(GuiGraphicsExtractor graphics, Minecraft client) {
        float strength = client.player == null ? 0.0F : client.player.getAttackStrengthScale(0.0F);
        if (strength >= 0.99F
                && !(client.hitResult instanceof EntityHitResult)
                && GumihoClient.MODULES.isEnabled("contextual_hud")
                && !(client.gui.screen() instanceof HudEditorScreen)) return;
        graphics.fill(x(), y(), x() + WIDTH, y() + hudHeight(), HudTheme.PANEL_BACKGROUND);
        graphics.fill(x(), y(), x() + 3, y() + hudHeight(), 0xFFFF7A18);
        int color = strength >= 1.0F ? 0xFF82E68A : 0xFFFFE8C2;
        String percent = Math.round(strength * 100.0F) + "%";
        graphics.text(client.font, "Attack | 공격", x() + 7, y() + 4, color, false);
        graphics.text(client.font, percent, x() + WIDTH - 7 - client.font.width(percent), y() + 4, color, false);
        int trackLeft = x() + 7;
        int trackRight = x() + WIDTH - 7;
        graphics.fill(trackLeft, y() + 17, trackRight, y() + 22, 0xFF34271F);
        graphics.fill(trackLeft, y() + 17,
                trackLeft + Math.round((trackRight - trackLeft) * strength), y() + 22, 0xFFFF7A18);
    }

    @Override public int hudWidth(Minecraft client) { return WIDTH; }
    @Override public int hudHeight() { return 27; }
}
