package dev.gumiho.client.module;

import dev.gumiho.client.gui.HudTheme;
import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.gui.HudEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.List;
/** A read-only panel whose inventory/effect data is sampled at most once per player tick. */
public abstract class SnapshotHudModule extends HudModule {
    protected static final int MAX_WIDTH=220;
    public record Row(String text,int color) { }
    private final String title;
    private final int maximumRows;
    private Object level, player;
    private int lastTick=Integer.MIN_VALUE, panelWidth=176;
    private List<Row> rows=List.of();
    protected SnapshotHudModule(String id,String en,String ko,String desc,String koDesc,ModuleCategory category,
                                boolean enabled,int x,int y,String title,int maximumRows) {
        super(id,en,ko,desc,koDesc,category,enabled,x,y);
        this.title=title; this.maximumRows=maximumRows;
    }
    protected abstract List<Row> readRows(Minecraft client);
    private void sample(Minecraft client) {
        int tick=client.player==null ? 0 : client.player.tickCount;
        if(level==client.level&&player==client.player&&lastTick==tick) return;
        level=client.level; player=client.player; lastTick=tick;
        rows=List.copyOf(readRows(client));
        panelWidth=Math.min(MAX_WIDTH,Math.max(176,Math.max(client.font.width(title),
                rows.stream().mapToInt(row->client.font.width(row.text())).max().orElse(0))+14));
    }
    @Override public void tick(Minecraft client) { sample(client); }
    @Override protected void onEnable(Minecraft client) { lastTick=Integer.MIN_VALUE; sample(client); }
    @Override protected void onDisable(Minecraft client) { lastTick=Integer.MIN_VALUE; rows=List.of(); level=null; player=null; }
    @Override public int hudWidth(Minecraft client) { sample(client); return panelWidth; }
    @Override public int hudHeight() { return dev.gumiho.client.gui.HudSizing.compactHeight(18,11,rows.size(),maximumRows); }
    @Override public void renderHud(GuiGraphics g,Minecraft c) {
        sample(c);
        if(rows.isEmpty()&&GumihoClient.MODULES.isEnabled("contextual_hud")&&!(c.screen instanceof HudEditorScreen)) return;
        g.fill(x(),y(),x()+panelWidth,y()+hudHeight(),HudTheme.PANEL_BACKGROUND);
        g.fill(x(),y(),x()+3,y()+hudHeight(),0xFFFF7A18);
        g.drawString(c.font,title,x()+7,y()+4,0xFFFFE8C2,false);
        if(rows.isEmpty()) g.drawString(c.font,"None | 없음",x()+7,y()+16,0xFF9E9187,false);
        for(int i=0;i<Math.min(maximumRows,rows.size());i++)
            g.drawString(c.font,fit(c,rows.get(i).text(),panelWidth-14),x()+7,y()+16+i*11,rows.get(i).color(),false);
    }
    protected static String fit(Minecraft c,String text,int width) {
        if(c.font.width(text)<=width) return text;
        return c.font.plainSubstrByWidth(text,Math.max(0,width-c.font.width("…")))+"…";
    }
}
