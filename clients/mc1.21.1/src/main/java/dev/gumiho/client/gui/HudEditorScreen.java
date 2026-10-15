package dev.gumiho.client.gui;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.module.HudModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import java.util.List;

/** Per-panel sizing without rearranging accepted layout positions. */
public final class HudEditorScreen extends Screen {
    private final Screen parent;
    private HudModule selected, dragging;
    private int offsetX,offsetY;
    private boolean showDisabled;
    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Editor | HUD 편집기")); this.parent=parent;
    }
    @Override protected void init() {
        int w=(width-32)/6,y=height-22;
        button(6,y,w,"Saved | 저장","Save or import this layout, including panel sizes. | 배치와 크기 저장",
                () -> minecraft.setScreen(new SavedSetupsScreen(this,dev.gumiho.client.profile.PresetCodec.Kind.HUD)));
        button(6+(w+4),y,w,showDisabled?"OFF: 표시":"OFF: 숨김",
                "Show/hide disabled panels in the editor only. Does not change module toggles. | 꺼진 패널 표시",
                () -> {showDisabled=!showDisabled; if(selected!=null&&!selected.enabled()&&!showDisabled)selected=null;rebuildWidgets();});
        button(6+2*(w+4),y,w,"−","Shrink the selected panel by 5%. | 선택 패널 축소",() -> resize(selected,-.05));
        button(6+3*(w+4),y,w,"+","Enlarge the selected panel by 5%. | 선택 패널 확대",() -> resize(selected,.05));
        button(6+4*(w+4),y,w,"Reset | 초기화","Reset all positions and sizes after confirmation. | 확인 후 전체 초기화",
                () -> minecraft.setScreen(new ConfirmScreen(yes -> {
                    if(yes)GumihoClient.MODULES.resetHudPositions();
                    minecraft.setScreen(this);
                },Component.literal("Reset HUD? | HUD 초기화?"),
                        Component.literal("This replaces your positions and sizes. Save your layout first."))));
        button(6+5*(w+4),y,w,"Done | 완료","Keep your layout and return. | 배치 유지",this::onClose);
    }
    private void button(int x,int y,int w,String label,String help,Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label),b -> action.run()).bounds(x,y,w,20)
                .tooltip(Tooltip.create(Component.literal(help))).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float tick) {
        super.renderBackground(g,mx,my,tick);
        // Light grid; only the selected panel receives another border/label.
        for(int x=0;x<width;x+=20)g.fill(x,0,x+1,height-38,0x142D211B);
        for(int y=0;y<height-38;y+=20)g.fill(0,y,width,y+1,0x142D211B);
        for(HudModule hud:hudModules()) {
            hud.prepareLayout(minecraft,width,height-40);
            hud.renderScaled(g,minecraft);
            if(hud==selected) {
                int l=hud.x()-1,t=hud.y()-1,r=hud.x()+hud.scaledWidth(minecraft)+1,b=hud.y()+hud.scaledHeight()+1;
                g.fill(l,t,r,t+1,0xFFFF7A18);g.fill(l,b-1,r,b,0xFFFF7A18);
                g.fill(l,t,l+1,b,0xFFFF7A18);g.fill(r-1,t,r,b,0xFFFF7A18);
            }
        }
        g.fill(0,height-38,width,height,0xEE120E0B);
        g.fill(0,height-38,width,height-36,0xFFFF7A18);
        String help=selected==null?"Select / drag • Scroll to resize | 선택 / 이동 / 크기":
                selected.name()+"  "+Math.round(selected.scale()*100)+"%  "+(selected.enabled()?"ON":"OFF");
        g.drawCenteredString(font,font.plainSubstrByWidth(help,width-12),width/2,height-33,0xFFFFB36B);
        super.render(g,mx,my,tick);
    }
    private void resize(HudModule hud,double delta) {
        if(hud==null)return;
        hud.setScale(Math.round((hud.scale()+delta)*100)/100.0);
        selected=hud;
        GumihoClient.MODULES.saveHudPosition(hud);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(my>=height-38 || vertical==0)return super.mouseScrolled(mx,my,horizontal,vertical);
        HudModule hud=hit(mx,my);
        if(hud==null)return super.mouseScrolled(mx,my,horizontal,vertical);
        resize(hud,Math.copySign(.05,vertical));
        return true;
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(super.mouseClicked(mx,my,button))return true;
        if(button!=0 || my>=height-38)return false;
        dragging=hit(mx,my); selected=dragging;
        if(dragging==null)return false;
        offsetX=(int)mx-dragging.x();offsetY=(int)my-dragging.y();
        return true;
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy) {
        if(dragging==null||button!=0)return super.mouseDragged(mx,my,button,dx,dy);
        dragging.setPosition(Mth.clamp((int)mx-offsetX,0,Math.max(0,width-dragging.scaledWidth(minecraft))),
                Mth.clamp((int)my-offsetY,0,Math.max(0,height-40-dragging.scaledHeight())));
        return true;
    }
    @Override public boolean mouseReleased(double mx,double my,int button) {
        if(dragging!=null&&button==0) {saveDrag();return true;}
        return super.mouseReleased(mx,my,button);
    }
    private void saveDrag() {
        if(dragging!=null)GumihoClient.MODULES.saveHudPosition(dragging);
        dragging=null;
    }
    private HudModule hit(double x,double y) {
        var modules=hudModules();
        for(int i=modules.size()-1;i>=0;i--) {
            var hud=modules.get(i);
            hud.prepareLayout(minecraft,width,height-40);
            if(x>=hud.x()&&x<=hud.x()+hud.scaledWidth(minecraft)
                    &&y>=hud.y()&&y<=hud.y()+hud.scaledHeight())return hud;
        }
        return null;
    }
    private List<HudModule> hudModules() {
        return GumihoClient.MODULES.modules().stream().filter(HudModule.class::isInstance)
                .map(HudModule.class::cast).filter(h -> showDisabled||h.enabled()).toList();
    }
    @Override public void onClose() {saveDrag();minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen() {return false;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float tick) { }
}
