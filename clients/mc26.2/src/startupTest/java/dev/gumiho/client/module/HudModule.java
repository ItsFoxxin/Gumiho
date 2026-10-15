package dev.gumiho.client.module;
import net.minecraft.client.Minecraft;
/** Lifecycle fixture only; HUD layout is covered separately by layout tests. */
public class HudModule extends GumihoModule {
    private int x=6,y=6;
    private double scale=1;
    public int renders;
    public void setScale(double value) {scale=value;}
    public double scale() {return scale;}
    public void renderScaled(net.minecraft.client.gui.GuiGraphicsExtractor graphics,Minecraft client) {renders++;renderHud(graphics,client);}
    public HudModule() { super("hud","Hud","HUD","","",ModuleCategory.VISUAL,true,false); }
    public void prepareLayout(Minecraft client,int width,int height) { }
    public void setPosition(int x,int y) {this.x=x;this.y=y;}
    public int preferredX() {return x;}
    public int preferredY() {return y;}
    public void resetPosition() {x=6;y=6;}
}
