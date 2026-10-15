package dev.gumiho.client.gui;

import dev.gumiho.client.GumihoClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

public final class CombatSettingsScreen extends Screen {
    private final Screen parent;
    private int page;
    public CombatSettingsScreen(Screen parent) {
        super(Component.literal("Combat Settings | 전투 설정"));
        this.parent = parent;
    }
    @Override protected void init() {
        var c = GumihoClient.CONFIG.config();
        int x = left() + 14;
        int y = top() + 48;
        int w = panelWidth() - 28;
        if (page == 0) {
            toggle(x, y, w, "Hostile Mobs | 적대적 몹", () -> c.targetHostileMobs, value -> c.targetHostileMobs = value);
            toggle(x, y + 28, w, "Marked Enemies | 지정 적", () -> c.targetEnemyPlayers, value -> c.targetEnemyPlayers = value);
            toggle(x, y + 56, w, "Passive Mobs | 비적대적 몹", () -> c.targetPassiveMobs, value -> c.targetPassiveMobs = value);
            toggle(x, y + 84, w, "Invisible Targets | 투명 대상", () -> c.targetInvisible, value -> c.targetInvisible = value);
            addRenderableWidget(Button.builder(priorityLabel(), button -> {
                c.targetPriority = switch (c.targetPriority) {
                    case "CROSSHAIR" -> "DISTANCE";
                    case "DISTANCE" -> "HEALTH";
                    default -> "CROSSHAIR";
                };
                button.setMessage(priorityLabel());
            }).tooltip(Tooltip.create(Component.literal(CombatOptionHelp.text("Priority")))).bounds(x, y + 112, w, 24).build());
        } else if (page == 1) {
            slider(x, y, w, "Target Range | 대상 거리", 4, 96, c.targetRange, "m", value -> c.targetRange = value);
            slider(x, y + 28, w, "Full FOV | 전체 시야각", 10, 360, c.targetFov, "°", value -> c.targetFov = value);
            slider(x, y + 56, w, "Melee Range | 근접 거리", 1, 6, c.meleeRange, "m", value -> c.meleeRange = value);
            slider(x, y + 84, w, "Melee Aim Speed | 근접 조준 속도", 0.5, 10, c.meleeAimSpeed, "°/tick", value -> c.meleeAimSpeed = value);
            slider(x, y + 112, w, "Aim Ease | 조준 완화", 50, 400, c.meleeAimResponse * 1000, "ms", value -> c.meleeAimResponse = value / 1000);
        } else if (page == 2) {
            toggle(x, y, w, "Weapons Only | 무기만 사용", () -> c.meleeWeaponsOnly, value -> c.meleeWeaponsOnly = value);
            toggle(x, y + 28, w, "Wait Hurt Animation | 피격 동작 대기", () -> c.meleeWaitHurt, value -> c.meleeWaitHurt = value);
            toggle(x, y + 56, w, "Avoid Raised Shield | 방패 회피", () -> c.meleeAvoidShields, value -> c.meleeAvoidShields = value);
            toggle(x, y + 84, w, "Detailed Target HUD | 상세 대상 HUD", () -> c.targetHudDetailed, value -> c.targetHudDetailed = value);
            toggle(x, y + 112, w, "Shield Attack Windows | 방패 공격 틈", () -> c.shieldAttackWindows, value -> c.shieldAttackWindows = value);
        } else if (page == 3) {
            slider(x, y, w, "Mace Minimum Fall | 철퇴 낙하", 1.6, 12, c.maceMinimumFall, "m", value -> c.maceMinimumFall = value);
            slider(x, y + 28, w, "Mace Lock Range | 철퇴 추적 거리", 6, 64, c.maceLockRange, "m", value -> c.maceLockRange = value);
            toggle(x, y + 56, w, "Landing Guide | 착지 안내", () -> c.maceLandingGuide, v -> c.maceLandingGuide = v);
            toggle(x, y + 84, w, "Mace Air Correction | 철퇴 공중 보정", () -> c.maceAirControl, v -> c.maceAirControl = v);
            slider(x, y + 112, w, "Reset Radius | 재설정 반경", 1, 8, c.maceResetRadius, "m", v -> c.maceResetRadius = v);
        }
        if (page == 4) {
            slider(x, y, w, "Shield Lead | 방패 준비", 5, 12, c.shieldLeadTicks, " ticks", value -> c.shieldLeadTicks = value);
            toggle(x, y + 28, w, "Shield Projectiles | 투사체 방어", () -> c.shieldProjectiles, value -> c.shieldProjectiles = value);
            toggle(x, y + 56, w, "React to Swings | 근접 반응", () -> c.shieldMeleeSwings, value -> c.shieldMeleeSwings = value);
            toggle(x, y + 84, w, "Close Melee Guard | 근접 방어", () -> c.shieldCloseGuard, v -> c.shieldCloseGuard = v);
            toggle(x, y + 112, w, "Explosion Guard | 폭발 방어", () -> c.shieldExplosions, v -> c.shieldExplosions = v);
        }
        int footer = top() + panelHeight() - 30;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> { page = (page + 4) % 5; rebuildWidgets(); })
                .bounds(x, footer, 32, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Done | 완료"), button -> onClose())
                .bounds(width / 2 - 55, footer, 110, 22).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> { page = (page + 1) % 5; rebuildWidgets(); })
                .bounds(x + w - 32, footer, 32, 22).build());
    }
    private Component priorityLabel() {
        String name = switch (GumihoClient.CONFIG.config().targetPriority) {
            case "DISTANCE" -> "Distance | 거리";
            case "HEALTH" -> "Low Health | 낮은 체력";
            default -> "Crosshair | 조준선";
        };
        return Component.literal("Priority | 우선순위: " + name);
    }
    private void toggle(int x, int y, int w, String name, BooleanSupplier get, Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(Component.literal(name + ": " + (get.getAsBoolean() ? "ON" : "OFF")), button -> {
            set.accept(!get.getAsBoolean());
            button.setMessage(Component.literal(name + ": " + (get.getAsBoolean() ? "ON" : "OFF")));
        }).tooltip(Tooltip.create(Component.literal(CombatOptionHelp.text(name)))).bounds(x, y, w, 24).build());
    }
    private void slider(int x, int y, int w, String name, double min, double max, double initial,
                        String suffix, DoubleConsumer setter) {
        var control = new GumihoSettingsScreen.NumericSlider(x, y, w, 24, min, max, initial,
                value -> Component.literal(name + ": " + String.format(Locale.ROOT, "%.1f", value) + suffix), setter);
        control.setTooltip(Tooltip.create(Component.literal(CombatOptionHelp.text(name))));
        addRenderableWidget(control);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left(), top(), left() + panelWidth(), top() + panelHeight(), 0xF3120E0B);
        g.fill(left(), top(), left() + panelWidth(), top() + 3, 0xFFFF7A18);
        String section = switch (page) {
            case 0 -> "Targets | 대상";
            case 1 -> "Aim / Range | 조준 / 거리";
            case 2 -> "Melee / HUD | 근접 / HUD";
            case 3 -> "Mace | 철퇴";
            default -> "Shield | 방패";
        };
        g.centeredText(font, "Combat | 전투: " + section + "  " + (page + 1) + "/5", width / 2, top() + 10, 0xFFFFE8C2);
        String hint = switch (page) {
            case 0 -> "Teammates and neutral players stay protected";
            case 1 -> "Melee never exceeds normal interaction reach";
            case 2 -> "Attack windows: click to release; auto waits for full charge.";
            case 3 -> "Manual movement wins; guides estimate landing/reset positions.";
            default -> "Shield must be in offhand; close threats may hold it.";
        };
        g.centeredText(font, font.plainSubstrByWidth(hint, panelWidth() - 24), width / 2, top() + 26, 0xFFFFA45B);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }
    private int panelWidth() { return Math.min(420, width - 24); }
    private int panelHeight() { return Math.min(236, height - 24); }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return (height - panelHeight()) / 2; }
    @Override public void onClose() { GumihoClient.CONFIG.save(); minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) { }
}
