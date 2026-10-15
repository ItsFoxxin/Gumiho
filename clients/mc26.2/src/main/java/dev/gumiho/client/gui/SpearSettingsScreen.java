package dev.gumiho.client.gui;

import dev.gumiho.client.GumihoClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class SpearSettingsScreen extends Screen {
    private final Screen parent;
    public SpearSettingsScreen(Screen parent) { super(Component.literal("Spear Options | 창 설정")); this.parent = parent; }
    @Override protected void init() {
        var c = GumihoClient.CONFIG.config();
        int x = (width - Math.min(340, width-36))/2, y = height/2-42;
        toggle(x,y,"Spear Aim | 창 조준", () -> c.spearAim, v -> c.spearAim=v);
        toggle(x,y+30,"Aim While Charging | 돌진 중 조준", () -> c.spearChargeAim, v -> c.spearChargeAim=v);
        toggle(x,y+60,"Allow Cooldown Jabs | 자동 찌르기 허용", () -> c.spearJab, v -> c.spearJab=v);
        addRenderableWidget(Button.builder(Component.literal("Done | 완료"), b -> onClose()).bounds(width/2-55,y+100,110,22).build());
    }
    private void toggle(int x,int y,String name,BooleanSupplier get,Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(Component.literal(name+": "+(get.getAsBoolean()?"ON":"OFF")), b -> {
            set.accept(!get.getAsBoolean()); b.setMessage(Component.literal(name+": "+(get.getAsBoolean()?"ON":"OFF")));
        }).bounds(x,y,Math.min(340,width-36),24).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial) {
        super.extractBackground(g,mx,my,partial);
        int half = Math.min(184, width/2-6);
        g.fill(width/2-half,height/2-96,width/2+half,height/2+88,0xF3120E0B);
        g.fill(width/2-half,height/2-96,width/2+half,height/2-93,0xFFFF7A18);
        g.centeredText(font,title,width/2,height/2-80,0xFFFFE8C2);
        g.centeredText(font,"Jabs: Cooldown Attack / Charge: manual",width/2,height/2-63,0xFFFFA45B);
        super.extractRenderState(g,mx,my,partial);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float t) { }
    @Override public void onClose() { GumihoClient.CONFIG.save(); minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
