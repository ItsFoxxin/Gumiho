package dev.gumiho.client.gui;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.gui.widget.GumihoCategoryButton;
import dev.gumiho.client.gui.widget.GumihoToggleButton;
import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GumihoScreen extends Screen {
    private static final int PANEL = 0xF3120E0B;
    private static final int SIDEBAR = 0xF01B1511;
    private static final int CARD = 0xE92B211B;
    private static final int CARD_ENABLED = 0xF13B2415;
    private static final int ORANGE = 0xFFFF7A18;
    private static final int CREAM = 0xFFFFE8C2;

    private final List<GumihoToggleButton> moduleButtons = new ArrayList<>();
    private ModuleCategory selectedCategory = ModuleCategory.MOVEMENT;
    private EditBox searchBox;
    private Button profileButton;
    private Button combatOptionsButton, sectionButton, linkButton;
    private CombatSection section = CombatSection.ALL;
    private String hoveredDescription = "";
    private long hoverStarted;
    private String query = "";
    private int scrollOffset;

    public GumihoScreen() {
        super(Component.literal("GUMIHO | 구미호"));
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int sidebarWidth = sidebarWidth();

        int categoryY = top + 50;
        for (ModuleCategory category : ModuleCategory.values()) {
            addRenderableWidget(new GumihoCategoryButton(
                    left + 10,
                    categoryY,
                    sidebarWidth - 20,
                    22,
                    Component.literal(category.label()),
                    () -> selectedCategory == category,
                    () -> selectCategory(category)
            ));
            categoryY += 26;
        }

        int contentLeft = left + sidebarWidth;
        combatOptionsButton = addRenderableWidget(Button.builder(Component.literal("Combat Options | 전투 옵션"),
                        button -> minecraft.setScreen(new CombatSettingsScreen(this)))
                .bounds(left + 10, top + panelHeight() - 28, sidebarWidth - 20, 22).build());
        combatOptionsButton.visible = selectedCategory == ModuleCategory.COMBAT;
        linkButton = addRenderableWidget(Button.builder(Component.literal("GUMIHO Link | 링크"),
                button -> minecraft.setScreen(new dev.gumiho.link.LinkScreen(this)))
                .bounds(left + 10, top + panelHeight() - 28, sidebarWidth - 20, 22).build());
        linkButton.visible = selectedCategory != ModuleCategory.COMBAT;
        int contentWidth = panelWidth() - sidebarWidth;
        searchBox = new EditBox(
                font,
                contentLeft + 12,
                top + 50,
                contentWidth - 24,
                20,
                Component.literal("Search modules | 모듈 검색")
        );
        searchBox.setMaxLength(48);
        searchBox.setValue(query);
        searchBox.setHint(Component.literal("Search modules | 모듈 검색"));
        searchBox.setResponder(value -> {
            query = value;
            scrollOffset = 0;
            refreshModuleButtons();
        });
        addRenderableWidget(searchBox);

        int footerY = top + panelHeight() - footerHeight();
        int footerLeft = contentLeft + 12;
        int footerGap = 4;
        int footerWidth = contentWidth - 24;
        int columns = HudLayout.footerColumns(footerWidth);
        int footerButtonWidth = (footerWidth - footerGap * (columns - 1)) / columns;

        profileButton = Button.builder(
                        Component.literal("Setups | 설정 모음"),
                        clicked -> minecraft.setScreen(new SavedSetupsScreen(this))
                )
                .bounds(footerLeft, footerY, footerButtonWidth, 22)
                .build();
        addRenderableWidget(profileButton);

        addRenderableWidget(Button.builder(
                        Component.literal("Players | 플레이어"),
                        clicked -> minecraft.setScreen(new PlayerListsScreen(this))
                )
                .bounds(footerLeft + footerButtonWidth + footerGap, footerY, footerButtonWidth, 22)
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("Options | 옵션"),
                        clicked -> minecraft.setScreen(new GumihoSettingsScreen(this))
                )
                .bounds(footerLeft + (footerButtonWidth + footerGap) * (2 % columns), footerY + (2 / columns) * 26, footerButtonWidth, 22)
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("HUD Editor | HUD 편집기"),
                        clicked -> minecraft.setScreen(new HudEditorScreen(this))
                )
                .bounds(footerLeft + (footerButtonWidth + footerGap) * (3 % columns), footerY + (3 / columns) * 26, footerButtonWidth, 22)
                .build());

        sectionButton = addRenderableWidget(Button.builder(Component.literal(section.label() + " >"), button -> {
            do { section = CombatSection.values()[(section.ordinal() + 1) % CombatSection.values().length]; }
            while (section != CombatSection.ALL && GumihoClient.MODULES.modules().stream()
                    .noneMatch(module -> module.category() == ModuleCategory.COMBAT && CombatSection.of(module.id()) == section));
            button.setMessage(Component.literal(section.label() + " >"));
            scrollOffset = 0;
            refreshModuleButtons();
        }).bounds(contentLeft + 12, top + 28, Math.min(136, contentWidth - 84), 18).build());
        sectionButton.visible = selectedCategory == ModuleCategory.COMBAT;
        refreshModuleButtons();
    }

    private void selectCategory(ModuleCategory category) {
        selectedCategory = category;
        combatOptionsButton.visible = category == ModuleCategory.COMBAT;
        linkButton.visible = category != ModuleCategory.COMBAT;
        sectionButton.visible = category == ModuleCategory.COMBAT;
        scrollOffset = 0;
        refreshModuleButtons();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && (selectedCategory == ModuleCategory.COMBAT || selectedCategory == ModuleCategory.UTILITY || selectedCategory == ModuleCategory.MOVEMENT)) {
            int left = panelLeft() + sidebarWidth() + 12;
            int y = panelTop() + 82;
            for (GumihoModule module : page(visibleModules())) {
                if (mouseX >= left && mouseX < panelLeft() + panelWidth() - 12 && mouseY >= y && mouseY < y + 42) {
                    if (module.id().equals("bridge_assist")) {
                        minecraft.setScreen(new BridgeSettingsScreen(this));
                        return true;
                    }
                    if (module.id().equals("bow_aim_assist") || module.id().equals("player_bow_aim")
                            || module.id().equals("bow_status_hud")) {
                        minecraft.setScreen(new RangedSettingsScreen(this));
                        return true;
                    } else if (module.id().equals("projectile_paths")) {
                        minecraft.setScreen(new ProjectilePathSettingsScreen(this));
                        return true;
                    }
                    if (selectedCategory == ModuleCategory.COMBAT) {
                        minecraft.setScreen(new CombatSettingsScreen(this));
                        return true;
                    }
                }
                y += 48;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void refreshModuleButtons() {
        for (GumihoToggleButton button : moduleButtons) {
            removeWidget(button);
        }
        moduleButtons.clear();

        List<GumihoModule> modules = visibleModules();
        scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, modules.size() - pageCapacity()));
        int contentRight = panelLeft() + panelWidth();
        int y = panelTop() + 82;
        for (GumihoModule module : page(modules)) {
            GumihoToggleButton button = new GumihoToggleButton(
                    contentRight - 86,
                    y + 10,
                    72,
                    22,
                    module,
                    () -> {
                        if (GumihoClient.MODULES.toggle(module, minecraft)) {
                            GumihoClient.PROFILES.markCustom();
                            if (profileButton != null) {
                                profileButton.setMessage(Component.literal("Setups | 설정 모음"));
                            }
                        }
                    }
            );
            moduleButtons.add(button);
            addRenderableWidget(button);
            y += 48;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<GumihoModule> modules = visibleModules();
        int previous = scrollOffset;
        if (scrollY < 0) scrollOffset++;
        if (scrollY > 0) scrollOffset--;
        scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, modules.size() - pageCapacity()));
        if (previous != scrollOffset) {
            refreshModuleButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private List<GumihoModule> visibleModules() {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        return GumihoClient.MODULES.modules().stream()
                .filter(module -> module.category() == selectedCategory)
                .filter(module -> selectedCategory != ModuleCategory.COMBAT || section == CombatSection.ALL || CombatSection.of(module.id()) == section)
                .sorted(java.util.Comparator.comparingInt(module -> selectedCategory == ModuleCategory.COMBAT ? CombatSection.of(module.id()).ordinal() : 0))
                .filter(module -> needle.isEmpty()
                        || module.name().toLowerCase(Locale.ROOT).contains(needle)
                        || module.description().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        int left = panelLeft();
        int top = panelTop();
        int right = left + panelWidth();
        int bottom = top + panelHeight();
        int contentLeft = left + sidebarWidth();

        graphics.fill(left, top, right, bottom, PANEL);
        graphics.fill(left, top, contentLeft, bottom, SIDEBAR);
        graphics.fill(left, top, right, top + 4, ORANGE);
        graphics.fill(contentLeft, top + 42, contentLeft + 1, bottom, 0xFF5B3420);

        graphics.drawString(font, "GUMIHO | 구미호", left + 14, top + 16, CREAM);
        String version = dev.gumiho.client.BuildInfo.LABEL;
        graphics.drawString(font, version, right - font.width(version) - 14, top + 16, 0xFFFFA45B);
        if (selectedCategory != ModuleCategory.COMBAT) graphics.drawString(font, selectedCategory.label(), contentLeft + 12, top + 32, ORANGE);

        int y = top + 82;
        List<GumihoModule> allModules = visibleModules();
        List<GumihoModule> modules = page(allModules);
        boolean descriptionHovered = false;
        for (GumihoModule module : modules) {
            int cardLeft = contentLeft + 12;
            int cardRight = right - 12;
            graphics.fill(cardLeft, y, cardRight, y + 42, module.enabled() ? CARD_ENABLED : CARD);
            graphics.fill(cardLeft, y, cardLeft + 3, y + 42, module.enabled() ? ORANGE : 0xFF674A38);
            int textWidth = Math.max(40, cardRight - cardLeft - 102);
            graphics.drawString(font, fit(module.name(), textWidth), cardLeft + 10, y + 8, CREAM);
            boolean hover = mouseX >= cardLeft + 10 && mouseX < cardLeft + 10 + textWidth && mouseY >= y && mouseY < y + 42;
            if (hover) {
                descriptionHovered = true;
                long now = System.nanoTime() / 1_000_000;
                if (!hoveredDescription.equals(module.id())) { hoveredDescription = module.id(); hoverStarted = now; }
                int offset = DescriptionScroll.offset(font.width(module.description()), textWidth, now - hoverStarted);
                graphics.enableScissor(cardLeft + 10, y + 22, cardLeft + 10 + textWidth, y + 34);
                graphics.drawString(font, module.description(), cardLeft + 10 - offset, y + 23, 0xFFD7BDA8);
                graphics.disableScissor();
            } else graphics.drawString(font, fit(module.description(), textWidth), cardLeft + 10, y + 23, 0xFFD7BDA8);
            y += 48;
        }

        if (!descriptionHovered) hoveredDescription = "";
        if (allModules.size() > pageCapacity()) {
            String page = (scrollOffset + 1) + "–" + Math.min(allModules.size(), scrollOffset + pageCapacity())
                    + " / " + allModules.size();
            graphics.drawString(font, page, right - font.width(page) - 14, top + 32, 0xFFB8A394, false);
        }

        graphics.fill(contentLeft + 12, bottom - footerHeight() - 6, right - 12, bottom - footerHeight() - 5, 0xFF5B3420);

        if (modules.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    "No matching modules | 일치하는 모듈이 없습니다",
                    contentLeft + (right - contentLeft) / 2,
                    top + 112,
                    0xFFD7BDA8
            );
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Rendered explicitly once at the beginning of render().
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int panelWidth() {
        return Math.min(700, width - 24);
    }

    private int panelHeight() {
        return Math.min(360, height - 24);
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return (height - panelHeight()) / 2;
    }

    private int sidebarWidth() {
        return Math.min(155, panelWidth() / 3);
    }

    private int footerHeight() { return HudLayout.footerHeight(panelWidth() - sidebarWidth() - 24); }

    private int pageCapacity() {
        return Math.max(1, (panelHeight() - 82 - footerHeight() - 10) / 48);
    }

    private List<GumihoModule> page(List<GumihoModule> modules) {
        int from = Math.min(scrollOffset, modules.size());
        int to = Math.min(modules.size(), from + pageCapacity());
        return modules.subList(from, to);
    }

    private String fit(String text, int width) {
        if (font.width(text) <= width) return text;
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
    }
}
