package dev.gumiho.client.gui;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.tracking.PlayerGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class PlayerListsScreen extends Screen {
    private static final int ORANGE = 0xFFFF7A18;
    private static final int CREAM = 0xFFFFE8C2;
    private final Screen parent;
    private EditBox nameBox;
    private String status = "Type a player name | 플레이어 이름 입력";
    private int scrollOffset;

    public PlayerListsScreen(Screen parent) {
        super(Component.literal("Player Lists | 플레이어 목록"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = left();
        int top = top();
        String draft = nameBox == null ? "" : nameBox.getValue();
        nameBox = new EditBox(font, left + 14, top + 42, panelWidth() - 28, 20, Component.literal("Player name"));
        nameBox.setHint(Component.literal("Player name | 플레이어 이름"));
        nameBox.setMaxLength(32);
        nameBox.setValue(draft);
        addRenderableWidget(nameBox);

        int buttonY = top + 68;
        int w = (panelWidth() - 46) / 4;
        int half = (panelWidth() - 34) / 2;
        addRenderableWidget(actionButton(left + 14, buttonY, w, "Team | 팀", () -> set(PlayerGroup.TEAMMATE)));
        addRenderableWidget(actionButton(left + 14 + w + 6, buttonY, w, "Enemy | 적", () -> set(PlayerGroup.ENEMY)));
        addRenderableWidget(actionButton(left + 14 + (w + 6)*2, buttonY, w, "Neutral | 중립", () -> set(PlayerGroup.NEUTRAL)));
        addRenderableWidget(actionButton(left + 14 + (w + 6)*3, buttonY, w, "Remove | 삭제", this::remove));
        addRenderableWidget(actionButton(left + 14, buttonY + 26, half, "Pin Target | 대상 고정", this::pin));
        addRenderableWidget(actionButton(left + 20 + half, buttonY + 26, half, "Clear Pin | 고정 해제", this::clearPin));
        addRenderableWidget(Button.builder(Component.literal("Done | 완료"), clicked -> onClose())
                .bounds(width / 2 - 50, top + panelHeight() - 28, 100, 20).build());
    }

    private Button actionButton(int x, int y, int width, String text, Runnable action) {
        return Button.builder(Component.literal(text), clicked -> action.run()).bounds(x, y, width, 20).build();
    }

    private void set(PlayerGroup group) {
        String name = enteredName();
        if (name == null) return;
        GumihoClient.PLAYER_GROUPS.setGroup(name, group);
        status = name + " → " + group.name().toLowerCase();
    }

    private void remove() {
        String name = enteredName();
        if (name == null) return;
        status = GumihoClient.PLAYER_GROUPS.remove(name) ? "Removed " + name : "No saved entry for " + name;
    }

    private void pin() {
        String name = enteredName();
        if (name == null) return;
        if (GumihoClient.PLAYER_GROUPS.groupOf(name) == PlayerGroup.TEAMMATE) {
            status = "Cannot pin a teammate | 팀원을 고정할 수 없습니다";
            return;
        }
        GumihoClient.PLAYER_GROUPS.setGroup(name, PlayerGroup.ENEMY);
        GumihoClient.PLAYER_GROUPS.pin(name);
        status = "Pinned enemy: " + name;
    }

    private void clearPin() {
        GumihoClient.PLAYER_GROUPS.clearPin();
        status = "Target pin cleared | 대상 고정 해제";
    }

    private String enteredName() {
        String name = nameBox.getValue().strip();
        if (name.isEmpty()) {
            status = "Enter a player name first | 먼저 이름을 입력하세요";
            return null;
        }
        return name;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int size = entries().size();
        if (scrollY < 0) scrollOffset++;
        if (scrollY > 0) scrollOffset--;
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, size - visibleRows())));
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int left = left();
        int top = top();
        graphics.fill(left, top, left + panelWidth(), top + panelHeight(), 0xF3120E0B);
        graphics.fill(left, top, left + panelWidth(), top + 3, ORANGE);
        graphics.centeredText(font, "Player Lists | 플레이어 목록", width / 2, top + 12, CREAM);
        String pin = GumihoClient.PLAYER_GROUPS.pinnedName();
        graphics.text(font, "Pinned | 고정: " + (pin.isEmpty() ? "None | 없음" : pin), left + 14, top + 28, 0xFFFFA45B, false);
        graphics.text(font, font.plainSubstrByWidth(status, panelWidth() - 28), left + 14, top + 120, 0xFFD7BDA8, false);

        List<Map.Entry<String, String>> entries = entries();
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, entries.size() - visibleRows())));
        int to = Math.min(entries.size(), scrollOffset + visibleRows());
        graphics.enableScissor(left + 14, top + 138, left + panelWidth() - 14, top + panelHeight() - 34);
        for (int index = scrollOffset; index < to; index++) {
            Map.Entry<String, String> entry = entries.get(index);
            int rowY = top + 138 + (index - scrollOffset) * 12;
            int color = switch (entry.getValue()) {
                case "TEAMMATE" -> 0xFF78E69A;
                case "ENEMY" -> 0xFFFF8A78;
                default -> 0xFFB8AEA6;
            };
            String marker = GumihoClient.PLAYER_GROUPS.isPinned(entry.getKey()) ? " [PIN]" : "";
            graphics.text(font, entry.getKey() + "  •  " + entry.getValue().toLowerCase() + marker,
                    left + 18, rowY, color, false);
        }
        graphics.disableScissor();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private int panelWidth() { return Math.min(380, width - 24); }
    private int panelHeight() { return Math.min(258, height - 24); }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return (height - panelHeight()) / 2; }
    private int visibleRows() { return Math.max(0, (panelHeight() - 176) / 12); }

    private List<Map.Entry<String, String>> entries() {
        return GumihoClient.PLAYER_GROUPS.entries().entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .toList();
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }
}
