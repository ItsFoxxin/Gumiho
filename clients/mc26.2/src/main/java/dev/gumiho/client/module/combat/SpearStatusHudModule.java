package dev.gumiho.client.module.combat;

import dev.gumiho.client.gui.HudTheme;

import java.util.Locale;
import dev.gumiho.client.gui.HudEditorScreen;
import dev.gumiho.client.module.HudModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.KineticWeapon;

public final class SpearStatusHudModule extends HudModule {
    public SpearStatusHudModule() {
        super("spear_status_hud", "Spear Status", "창 상태",
                "Native jab readiness, reach band and estimated charge/speed status",
                "찌르기 준비, 사거리와 돌진 속도 상태", ModuleCategory.COMBAT, false, 6, 168);
    }
    @Override public void renderHud(GuiGraphicsExtractor g, Minecraft c) {
        boolean preview = c.gui.screen() instanceof HudEditorScreen;
        if (c.player == null) return;
        var stack = c.player.getMainHandItem();
        if (!preview && !SpearSupport.isSpear(stack)) return;
        String line1 = "Spear | 창: Jab " + Math.round(c.player.getAttackStrengthScale(0)*100) + "%";
        String line2 = "Hold Use to charge | 사용 키 유지";
        String line3 = "Reach | 사거리: 2.0-4.5m";
        if (SpearSupport.isSpear(stack)) {
            var range = c.player.getAttackRangeWith(stack);
            line3 = String.format(Locale.ROOT, "Reach | 사거리: %.1f-%.1fm", range.effectiveMinRange(c.player), range.effectiveMaxRange(c.player));
            var kinetic = stack.get(DataComponents.KINETIC_WEAPON);
            if (c.player.isUsingItem() && c.player.getUseItem() == stack && kinetic != null) {
                var condition = kinetic.damageConditions().orElse(null);
                var look = c.player.getLookAngle();
                double speed = look.dot(KineticWeapon.getMotion(c.player));
                var target = SpearSupport.hits(c).stream().findFirst().orElse(null);
                double relative = target == null ? speed : Math.max(0, speed-look.dot(KineticWeapon.getMotion(target.getEntity())));
                String state = condition == null ? "NO DAMAGE MODE" : SpearRules.thrustState(c.player.getTicksUsingItem(),
                        kinetic.delayTicks(), condition.maxDurationTicks(), speed, relative, condition.minSpeed(), condition.minRelativeSpeed());
                line2 = "Charge | 돌진: " + state + (target == null ? " ~" : "");
            } else if (SpearSupport.enabled(stack)) {
                line2 = SpearSupport.safeTarget(c) == null ? "No safe jab line | 안전 대상 없음" : "Safe jab line | 안전한 찌르기";
            }
        }
        g.fill(x(), y(), x()+hudWidth(c), y()+hudHeight(), HudTheme.PANEL_BACKGROUND);
        g.fill(x(), y(), x()+3, y()+hudHeight(), 0xFFFF7A18);
        g.text(c.font, line1, x()+7, y()+4, 0xFFFFE8C2, false);
        g.text(c.font, line2, x()+7, y()+15, 0xFFFFA45B, false);
        g.text(c.font, line3, x()+7, y()+26, 0xFFFFE8C2, false);
    }
    @Override public int hudWidth(Minecraft c) { return 230; }
    @Override public int hudHeight() { return 39; }
}
