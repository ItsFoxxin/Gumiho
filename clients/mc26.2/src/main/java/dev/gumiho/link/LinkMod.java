package dev.gumiho.link;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Path;
import java.util.*;
import static dev.gumiho.link.LinkData.*;

@Mod(value="gumiho_link",dist=Dist.CLIENT)
public final class LinkMod {
    public static final LinkClient CLIENT=new LinkClient();
    public static final Path PRIVATE_FILE=FMLPaths.CONFIGDIR.get().resolve("gumiho-link-private.json");
    public static LinkSettings settings=new LinkSettings();
    public static String fileStatus="";
    public static final int BACKGROUND=0xD0120E0B, ORANGE=0xFFFF7A18, CREAM=0xFFFFE8C2;
    private static final KeyMapping.Category CATEGORY=KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("gumiho_link","main"));
    public static final KeyMapping HOLD=new KeyMapping("key.gumiho_link.hold",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_ALT,CATEGORY);
    public static final KeyMapping OPTIONS=new KeyMapping("key.gumiho_link.options",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F8,CATEGORY);
    public static final KeyMapping PING=new KeyMapping("key.gumiho_link.ping",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_LEFT_ALT,CATEGORY);
    public LinkMod(IEventBus bus) {
        try { settings=LinkSettings.load(PRIVATE_FILE); }
        catch(Exception ignored) { fileStatus="Private settings unreadable; original file unchanged"; }
        bus.addListener(this::keys);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::render);
        // No Minecraft singleton access here: constructor can run before the client exists.
    }
    private void keys(RegisterKeyMappingsEvent e) { e.register(HOLD); e.register(OPTIONS); e.register(PING); }
    private void tick(ClientTickEvent.Post e) {
        Minecraft c=Minecraft.getInstance();
        if(c==null || c.options==null) return;
        if(CLIENT.requested()) CLIENT.sample(capture(c));
        while(OPTIONS.consumeClick()) if(c.gui.screen()==null) c.gui.setScreen(new LinkScreen(null));
        while(PING.consumeClick()) if(c.gui.screen()==null && c.player!=null) c.gui.setScreen(new LinkScreen(null,2,pingPoint(c)));
    }
    public static Sample capture(Minecraft c) {
        if(c.player==null || c.level==null || c.hasSingleplayerServer() || c.getCurrentServer()==null) return Sample.empty();
        var p=c.player; double remaining=0,max=0;
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)) {
            var item=p.getItemBySlot(slot);
            if(item.isDamageableItem()) { max+=item.getMaxDamage(); remaining+=Math.max(0,item.getMaxDamage()-item.getDamageValue()); }
        }
        return new Sample(new Context(serverKey(c.getCurrentServer().ip),c.level.dimension().identifier().toString()),
                new Position(p.getX(),p.getY(),p.getZ()),
                new Stats(Math.max(0,p.getHealth()),p.getMaxHealth(),p.getFoodData().getFoodLevel(),max==0?null:100*remaining/max,
                        BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).toString(),
                        BuiltInRegistries.ITEM.getKey(p.getOffhandItem().getItem()).toString()));
    }
    public static Position pingPoint(Minecraft c) {
        var v=c.hitResult!=null && c.hitResult.getType()!=HitResult.Type.MISS ? c.hitResult.getLocation() : c.player.position();
        return new Position(v.x,v.y,v.z);
    }
    public static String point(Position p) { return String.format(Locale.ROOT,"%.0f / %.0f / %.0f",p.x(),p.y(),p.z()); }
    private void render(RenderGuiEvent.Post e) {
        Minecraft c=Minecraft.getInstance();
        if(c==null || c.player==null || c.font==null || c.gui.screen()!=null || c.gui.hud.isHidden() || !HOLD.isDown()) return;
        GuiGraphicsExtractor g=e.getGuiGraphics(); View v=CLIENT.view();
        int w=Math.min(440,c.getWindow().getGuiScaledWidth()-16), x=(c.getWindow().getGuiScaledWidth()-w)/2;
        int maxLines=Math.max(1,(c.getWindow().getGuiScaledHeight()-76)/12);
        List<String> lines=new ArrayList<>();
        for(Member m:v.members()) {
            String detail=m.stats()==null?"stats private":String.format(Locale.ROOT,"HP %.0f/%.0f · Food %d",m.stats().health(),m.stats().maxHealth(),m.stats().hunger());
            lines.add(m.name()+" ["+m.role()+"] "+(m.online()?detail:"offline"));
            if(m.location()!=null) lines.add("  XYZ "+point(m.location()));
        }
        for(Ping p:v.pings()) lines.add(p.kind()+" → "+point(p.position()));
        if(lines.isEmpty()) lines.add(OPTIONS.getTranslatedKeyMessage().getString()+": Connection / sharing | 연결 / 공유");
        int shown=Math.min(maxLines,lines.size()), h=58+shown*12;
        g.fill(x,12,x+w,12+h,BACKGROUND); g.fill(x,12,x+w,15,ORANGE);
        draw(g,c,"GUMIHO Link | 구미호 · "+v.team(),x+10,22,w-20,CREAM);
        draw(g,c,CLIENT.status(),x+10,35,w-20,ORANGE);
        for(int i=0;i<shown;i++) draw(g,c,lines.get(i),x+10,50+i*12,w-20,CREAM);
        draw(g,c,lines.size()>shown?"More: "+OPTIONS.getTranslatedKeyMessage().getString()+" Team page | 더 보기":"Shared by members; not verified | 멤버 제공 정보",x+10,52+shown*12,w-20,ORANGE);
    }
    private static void draw(GuiGraphicsExtractor g,Minecraft c,String text,int x,int y,int w,int color) {
        g.text(c.font,c.font.plainSubstrByWidth(text,w),x,y,color,false);
    }
}
