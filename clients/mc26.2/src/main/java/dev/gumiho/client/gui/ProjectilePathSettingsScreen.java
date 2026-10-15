package dev.gumiho.client.gui;

import dev.gumiho.client.GumihoClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

public final class ProjectilePathSettingsScreen extends Screen {
    private final Screen parent;
    private int page;
    public ProjectilePathSettingsScreen(Screen parent) {
        super(Component.literal("Projectile Paths | 투사체 궤적")); this.parent = parent;
    }
    @Override protected void init() {
        var c = GumihoClient.CONFIG.config();
        int x = left() + 14, y = top() + 48, w = panelWidth() - 28;
        if (page == 0) {
            slider(x, y, w, "Range | 거리", 16, 128, c.projectilePathRange, "m", v -> c.projectilePathRange = v);
            slider(x, y + 28, w, "Prediction | 예측", 10, 80, c.projectilePathTicks, " ticks", v -> c.projectilePathTicks = v);
            slider(x, y + 56, w, "Max Paths | 최대 궤적", 1, 24, c.projectilePathLimit, "", v -> c.projectilePathLimit = v);
        } else if (page == 1) {
            toggle(x, y, w, "My Projectiles | 내 투사체", () -> c.projectilePathsOwn, v -> c.projectilePathsOwn = v);
            toggle(x, y + 28, w, "Other Projectiles | 다른 투사체", () -> c.projectilePathsOthers, v -> c.projectilePathsOthers = v);
            toggle(x, y + 56, w, "Impact Markers | 충돌 지점", () -> c.projectileImpactMarkers, v -> c.projectileImpactMarkers = v);
        }
        if (page == 2) {
            toggle(x, y, w, "Held Preview | 발사 전 예측", () -> c.projectileHeldPreview, v -> c.projectileHeldPreview = v);
        }
        int footer = top() + panelHeight() - 30;
        addRenderableWidget(Button.builder(Component.literal(page == 0 ? "Filters >" : page == 1 ? "Preview >" : "Limits >"), b -> {
            page = (page + 1) % 3; rebuildWidgets();
        }).bounds(x, footer, 100, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Done | 완료"), b -> onClose()).bounds(x + w - 100, footer, 100, 22).build());
    }
    private void slider(int x, int y, int w, String label, double min, double max, double value, String unit, DoubleConsumer set) {
        addRenderableWidget(new GumihoSettingsScreen.NumericSlider(x, y, w, 24, min, max, value,
                v -> Component.literal(label + ": " + Math.round(v) + unit), v -> set.accept(Math.round(v))));
    }
    private void toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(Component.literal(label + ": " + (get.getAsBoolean() ? "ON" : "OFF")), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(Component.literal(label + ": " + (get.getAsBoolean() ? "ON" : "OFF")));
        }).bounds(x, y, w, 24).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left(), top(), left() + panelWidth(), top() + panelHeight(), 0xF3120E0B);
        g.fill(left(), top(), left() + panelWidth(), top() + 3, 0xFFFF7A18);
        g.centeredText(font, title, width / 2, top() + 10, 0xFFFFE8C2);
        g.centeredText(font, "Trajectory estimates | 궤적 예측  " + (page + 1) + "/3", width / 2, top() + 26, 0xFFFFA45B);
        String legend = "Gold: arrows | Orange: fire | Cyan: wind/trident";
        g.centeredText(font, font.plainSubstrByWidth(legend, panelWidth() - 24), width / 2, top() + 144, 0xFFFFD56A);
        g.centeredText(font, font.plainSubstrByWidth("Purple: potions | Green: throws | Red X: entity", panelWidth() - 24), width / 2, top() + 157, 0xFFD395FA);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }
    private int panelWidth() { return Math.min(420, width - 24); }
    private int panelHeight() { return Math.min(212, height - 24); }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return (height - panelHeight()) / 2; }
    @Override public void onClose() { GumihoClient.CONFIG.save(); minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float tick) { }
}
