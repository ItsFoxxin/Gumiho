package dev.gumiho.client.gui;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.profile.PresetCodec;
import dev.gumiho.client.profile.SavedSetups;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.io.IOException;
import java.util.List;

public final class SavedSetupsScreen extends Screen {
    private final Screen parent;
    private PresetCodec.Kind kind = PresetCodec.Kind.KIT;
    private EditBox name;
    private String draft = "", status = "";
    public SavedSetupsScreen(Screen parent) {
        super(Component.literal("Saved Setups | 설정 모음")); this.parent = parent;
    }
    public SavedSetupsScreen(Screen parent, PresetCodec.Kind kind) { this(parent); this.kind = kind; }
    @Override protected void init() {
        int x = left() + 12, w = panelWidth() - 24, half = (w - 4) / 2, y = top() + 40;
        button(x,y,half,kind == PresetCodec.Kind.KIT ? "Kit | 키트" : "HUD Layout | HUD 배치",
                "Kit: options, toggles and HUD. HUD: positions and sizes. Click to switch.",
                () -> { kind = kind == PresetCodec.Kind.KIT ? PresetCodec.Kind.HUD : PresetCodec.Kind.KIT;
                    draft = ""; status = ""; rebuildWidgets(); });
        if (kind == PresetCodec.Kind.KIT) button(x+half+4,y,half,GumihoClient.PROFILES.current().label(),
                "Built-in preset: click to cycle. Save New stores your current custom setup.",
                () -> { GumihoClient.PROFILES.cycle(minecraft); rebuildWidgets(); });
        name = new EditBox(font,x,y+28,w,20,Component.literal("Setup name | 이름"));
        name.setMaxLength(40);
        name.setHint(Component.literal("Name: Bow kit, Mace, 구미호..."));
        name.setValue(draft);
        name.setResponder(value -> draft = value);
        addRenderableWidget(name);
        button(x,y+54,half,"< Previous | 이전","Browse saved files for the selected type.",() -> browse(-1));
        button(x+half+4,y+54,half,"Next | 다음 >","Browse saved files for the selected type.",() -> browse(1));
        button(x,y+80,half,"Save New | 새 저장","Never overwrites an existing name. Use a new name to save a variation.",
                () -> run(() -> { SavedSetups.save(draft,kind); status = "Saved | 저장 완료"; }));
        button(x+half+4,y+80,half,"Load | 불러오기","Load this name; asks for confirmation and saves a backup first.",
                () -> run(() -> confirm(SavedSetups.STORE.read(draft,kind),kind)));
        button(x,y+106,half,"Copy HUD | HUD 복사","Copies HUD positions and sizes as portable JSON, not module toggles.",
                () -> run(() -> { minecraft.keyboardHandler.setClipboard(SavedSetups.export(PresetCodec.Kind.HUD));
                    status = "HUD copied | HUD 복사 완료"; }));
        button(x+half+4,y+106,half,"Paste HUD | HUD 붙이기","Paste a HUD export from another GUMIHO version. Unknown modules are ignored.",
                () -> run(() -> confirm(minecraft.keyboardHandler.getClipboard(),PresetCodec.Kind.HUD)));
        button(width/2-50,top()+panelHeight()-28,100,"Done | 완료","Return to the menu.",this::onClose);
    }
    private void browse(int direction) {
        run(() -> {
            List<String> names = SavedSetups.STORE.names(kind);
            if (names.isEmpty()) { status = "No saved files | 저장 파일 없음"; return; }
            int current = names.indexOf(draft);
            int index = current < 0 ? (direction > 0 ? 0 : names.size()-1)
                    : Math.floorMod(current+direction,names.size());
            name.setValue(names.get(index));
            status = (index+1) + " / " + names.size() + " saved";
        });
    }
    private void confirm(String json, PresetCodec.Kind requestedKind) {
        PresetCodec.merge(json,requestedKind,SavedSetups.snapshot()); // Validate before asking or changing anything.
        minecraft.gui.setScreen(new ConfirmScreen(yes -> {
            if (yes) run(() -> status = SavedSetups.load(json,requestedKind,minecraft));
            minecraft.gui.setScreen(this);
        },Component.literal(requestedKind == PresetCodec.Kind.HUD ? "Apply HUD layout? | HUD 적용?" : "Load kit setup? | 키트 적용?"),
                Component.literal("A backup is saved first. Server permissions and player lists stay unchanged.")));
    }
    private void button(int x,int y,int w,String text,String help,Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(text),b -> action.run()).bounds(x,y,w,22)
                .tooltip(Tooltip.create(Component.literal(help))).build());
    }
    @FunctionalInterface private interface Operation { void run() throws IOException; }
    private void run(Operation action) {
        try { action.run(); }
        catch (IOException | RuntimeException error) {
            status = error instanceof java.nio.file.FileAlreadyExistsException
                    ? "Name exists; use a new name | 다른 이름 사용" : "Failed: " + error.getMessage();
            GumihoClient.LOGGER.warn("Saved setup operation failed",error);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick) {
        super.extractBackground(g,mx,my,tick);
        g.fill(left(),top(),left()+panelWidth(),top()+panelHeight(),0xF3120E0B);
        g.fill(left(),top(),left()+panelWidth(),top()+3,0xFFFF7A18);
        g.centeredText(font,title,width/2,top()+10,0xFFFFE8C2);
        g.centeredText(font,"Kits + portable layouts | 키트 + 배치",width/2,top()+24,0xFFFFA45B);
        g.centeredText(font,font.plainSubstrByWidth(status,panelWidth()-24),width/2,top()+174,0xFFFFA45B);
        super.extractRenderState(g,mx,my,tick);
    }
    private int panelWidth() { return Math.min(420,width-24); }
    private int panelHeight() { return Math.min(224,height-16); }
    private int left() { return (width-panelWidth())/2; }
    private int top() { return (height-panelHeight())/2; }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float tick) { }
}
