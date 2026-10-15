package dev.gumiho.link;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;
import java.util.function.Consumer;
import static dev.gumiho.link.LinkData.*;

/** Paginated small-screen controls, deliberately separate from exported GUMIHO setups. */
public final class LinkScreen extends Screen {
    private final Screen parent;
    private int page,memberIndex,kind;
    private final Position point;
    private final Context pingContext;
    private String endpoint=LinkMod.settings.endpoint, credential=LinkMod.settings.memberToken, name="", team="";
    private String note=LinkMod.fileStatus;
    private boolean busy, browsePings;
    private final List<String> audience=new ArrayList<>();
        public LinkScreen(Screen parent) { this(parent,0,null); }
    public LinkScreen(Screen parent,int page,Position point) {
        super(Component.literal("GUMIHO Link | 구미호")); this.parent=parent; this.page=page; this.point=point;
        var c=net.minecraft.client.Minecraft.getInstance();
        pingContext=c==null?null:LinkMod.capture(c).context();
    }
    @Override protected void init() {
        int x=left()+10,w=panelWidth()-20,y=top()+48,half=(w-4)/2;
        if(width<320 || height<240) {
            button(width/2-50,height-28,100,"Done | 완료","",this::onClose); return;
        }
        String[] tabs={"Connect | 연결","Share | 공유","Ping | 핑","Team | 팀"};
        for(int i=0;i<4;i++) { int tab=i; button(x+i*(w/4),top()+26,w/4-3,tabs[i],"",() -> { page=tab; rebuildWidgets(); }); }
        if(page==0) {
            field(x,y,w,"Relay URL | 릴레이 주소",endpoint,v -> endpoint=v,false);
            field(x,y+24,w,"Member / invite / owner token | 인증 키",credential,v -> credential=v,true);
            field(x,y+48,half,"Display name | 표시 이름",name,v -> name=v,false);
            field(x+half+4,y+48,half,"New team name | 새 팀 이름",team,v -> team=v,false);
            button(x,y+70,half,"Save + Connect | 연결","Saves a private plaintext member key on this computer. Never share this file or put it in a modpack.",this::connect);
            button(x+half+4,y+70,half,"Disconnect | 연결 끊기","Clear local data now; relay data expires within five seconds if unreachable.",() -> { LinkMod.CLIENT.disconnect(); note="Disconnected; local data cleared"; });
            button(x,y+94,w,"Join / Create | 가입 / 생성","Paste gli_ invite to join; glb_ owner key to create. Does not auto-connect or upload.",this::provision);
            button(x,y+116,w,"Forget member key | 키 삭제","Disconnect and remove saved member credential. Does not leave or delete your relay team.",() -> {
                LinkMod.CLIENT.disconnect(); credential=""; LinkMod.settings.memberToken=""; save();
            });
        } else if(page==1) {
            for(int i=0;i<ROLES.size();i++) {
                String role=ROLES.get(i); var sharing=LinkMod.settings.sharing;
                button(x,y+i*24,half,on(sharing.locationRoles().contains(role))+" XYZ "+role,"Exact role only. Founder has no visibility bypass. Changing disconnects; reconnect to apply.",() -> toggle(role,true));
                button(x+half+4,y+i*24,half,on(sharing.statsRoles().contains(role))+" Stats "+role,"Own health, hunger, armor percent, held item IDs only; never inventory contents.",() -> toggle(role,false));
            }
            button(x,y+116,w,"All sharing OFF | 공유 모두 끄기","Immediately disconnect, then clear both audiences.",() -> {
                LinkMod.CLIENT.disconnect(); LinkMod.settings.sharing=Sharing.none(); save(); rebuildWidgets();
            });
        } else if(page==2) {
            button(x,y,w,KINDS.get(kind)+" · "+(point==null?"Use Ping key | 핑 키":LinkMod.point(point)),"The Ping key captures the looked-at hit point, or your position if aiming at empty air.",() -> { kind=(kind+1)%KINDS.size(); rebuildWidgets(); });
            for(int i=0;i<ROLES.size();i++) {
                String role=ROLES.get(i);
                button(x+(i%2)*(half+4),y+22+(i/2)*24,half,on(audience.contains(role))+" "+role,"This ping only: choose exact recipient roles.",() -> { if(!audience.remove(role)) audience.add(role); rebuildWidgets(); });
            }
            button(x,y+92,w,"Send this point | 이 위치 보내기","WARNING: sends this exact point even when continuous location sharing is OFF. Expires after 30 seconds.",() -> {
                var current=LinkMod.capture(minecraft).context();
                if(point==null || pingContext==null || !Objects.equals(pingContext,current)) note="Use the Ping key in the current multiplayer world first";
                else note=LinkMod.CLIENT.ping(point,KINDS.get(kind),audience)?"Ping queued; shares point even with XYZ OFF":"Connect and choose an audience first";
            });
            button(x,y+116,w,"Clear my pings | 내 핑 삭제","Removes your active pings after the next successful request.",() -> note=LinkMod.CLIENT.clearPings()?"Clear queued":"Not connected");
        } else {
            button(x,y,half,"< Previous | 이전","Browse every team member even on small screens.",() -> { memberIndex=Math.max(0,memberIndex-1); });
            button(x+half+4,y,half,"Next | 다음 >","",() -> memberIndex++);
            button(x,y+92,half,browsePings?"View: Pings | 핑":"View: Members | 멤버",
                    "Switch roster and active ping details. Names/stats are member-reported, not verified.",
                    () -> { browsePings=!browsePings; memberIndex=0; rebuildWidgets(); });
            button(x+half+4,y+92,half,"Manage | 관리","Promote, demote, remove, rotate your key, or transfer Founder with confirmations.",
                    () -> minecraft.setScreen(new LinkMemberScreen(this)));
            Identity identity=LinkMod.CLIENT.identity();
            if(identity!=null && LinkPolicy.canInvite(identity.role()))
                button(x,y+116,w,"Copy single-use invite | 초대 복사","Grants MEMBER membership; never grants combat features. Clipboard contains a secret invite.",() -> {
                    note=LinkMod.CLIENT.invite(value -> minecraft.execute(() -> { minecraft.keyboardHandler.setClipboard(value); note="Invite copied; share privately (10 minute expiry)"; }))?"Requesting invite":"Connect first";
                });
            else button(x,y+116,w,"Invite requires Officer+ | 초대 권한 필요","Only Founder, Co-founder and Officer roles may create invitations.",() -> note="Your role cannot create invitations");
        }
        button(left()+10,top()+panelHeight()-24,86,"Keybinds | 키 설정","Change Right Alt (Link), Left Alt (ping), F8 (settings) under GUMIHO Link.",
                () -> minecraft.setScreen(new net.minecraft.client.gui.screens.options.controls.KeyBindsScreen(this,minecraft.options)));
        button(width/2-50,top()+panelHeight()-24,100,"Done | 완료","",this::onClose);
    }
    private String on(boolean value) { return value?"ON":"OFF"; }
    private void field(int x,int y,int w,String hint,String value,Consumer<String> responder,boolean secret) {
        EditBox field=new EditBox(font,x,y,w,20,Component.literal(hint)); field.setMaxLength(secret?64:160);
        if(secret) field.setFormatter((s,index) -> FormattedCharSequence.forward("*".repeat(s.length()),Style.EMPTY));
        field.setHint(Component.literal(hint)); field.setValue(value); field.setResponder(responder); addRenderableWidget(field);
    }
    private void button(int x,int y,int w,String label,String help,Runnable action) {
        Button b=Button.builder(Component.literal(font.plainSubstrByWidth(label,w-8)),ignored -> { if(!busy) action.run(); })
                .bounds(x,y,w,18).tooltip(Tooltip.create(Component.literal(help.isEmpty()?label:label+"\n"+help))).build();
        addRenderableWidget(b);
    }
    private void toggle(String role,boolean location) {
        LinkMod.CLIENT.disconnect(); Sharing old=LinkMod.settings.sharing;
        List<String> loc=new ArrayList<>(old.locationRoles()), stats=new ArrayList<>(old.statsRoles());
        List<String> list=location?loc:stats; if(!list.remove(role)) list.add(role);
        LinkMod.settings.sharing=new Sharing(loc,stats); save(); rebuildWidgets();
    }
    private boolean save() {
        try { LinkMod.settings.save(LinkMod.PRIVATE_FILE); note="Saved; reconnect to apply | 저장됨"; return true; }
        catch(Exception ignored) { note="Save failed; check private config file"; return false; }
    }
    private void connect() {
        LinkMod.CLIENT.disconnect();
        try {
            LinkMod.settings.endpoint=LinkData.endpoint(endpoint).toString();
            LinkMod.settings.memberToken=LinkData.token(credential,"gl1_");
            if(save()) { LinkMod.CLIENT.sample(LinkMod.capture(minecraft)); LinkMod.CLIENT.connect(endpoint,credential,LinkMod.settings.sharing); }
        } catch(RuntimeException ignored) { note="Use HTTPS or local 127.0.0.1, and a gl1_ member key"; }
    }
    private void provision() {
        try {
            busy=true; note="Joining / creating; please wait";
            LinkMod.CLIENT.provision(endpoint,credential,name,team).whenComplete((token,error) -> minecraft.execute(() -> {
                busy=false;
                if(error!=null) { note="Join/create failed. Check key, names and relay."; return; }
                credential=token; LinkMod.settings.memberToken=token; LinkMod.settings.endpoint=endpoint;
                LinkMod.settings.sharing=Sharing.none();
                if(save()) note="Member key saved. Sharing OFF. Press Connect.";
                rebuildWidgets();
            }));
        } catch(RuntimeException ignored) { busy=false; note="Paste gli_ invite or glb_ owner key; check relay URL"; }
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        super.renderBackground(g,mx,my,partial);
        g.fill(left(),top(),left()+panelWidth(),top()+panelHeight(),LinkMod.BACKGROUND);
        g.fill(left(),top(),left()+panelWidth(),top()+3,LinkMod.ORANGE);
        line(g,"GUMIHO Link | 구미호 · local test",top()+10,LinkMod.CREAM);
        if(width<320 || height<240) { line(g,"Lower GUI Scale to use Link settings",height/2,LinkMod.ORANGE); }
        else {
            if(page==3) {
                View v=LinkMod.CLIENT.view(); var rows=v.members();
                if(!browsePings) memberIndex=Math.max(0,Math.min(memberIndex,rows.size()-1));
                int y=top()+82;
                if(browsePings) {
                    var pings=v.pings(); memberIndex=Math.max(0,Math.min(memberIndex,pings.size()-1));
                    if(pings.isEmpty()) line(g,"No active shared pings | 공유 핑 없음",y,LinkMod.CREAM);
                    else {
                        Ping p=pings.get(memberIndex);
                        String sender=rows.stream().filter(m -> m.id().equals(p.senderId())).map(Member::name).findFirst().orElse("Teammate");
                        line(g,(memberIndex+1)+"/"+pings.size()+" "+p.kind()+" · "+sender,y,LinkMod.CREAM);
                        line(g,"XYZ "+LinkMod.point(p.position()),y+20,LinkMod.ORANGE);
                        line(g,"Expires in "+Math.max(0,(p.deadline()-System.nanoTime())/1000000000L)+"s",y+40,LinkMod.CREAM);
                    }
                } else if(rows.isEmpty()) line(g,"No roster. Connect first | 연결 필요",y,LinkMod.CREAM);
                else {
                    Member m=rows.get(memberIndex);
                    line(g,(memberIndex+1)+"/"+rows.size()+" "+m.name()+" · "+m.role(),y,LinkMod.CREAM);
                    line(g,m.online()?"Online | 접속 중":"Offline | 오프라인",y+16,LinkMod.ORANGE);
                    line(g,m.location()==null?"Location private / unavailable":"XYZ "+LinkMod.point(m.location()),y+32,LinkMod.CREAM);
                    line(g,m.stats()==null?"Stats private / unavailable":String.format(Locale.ROOT,"HP %.0f/%.0f · Food %d · Armor %s",m.stats().health(),m.stats().maxHealth(),m.stats().hunger(),m.stats().armorPercent()==null?"--":Math.round(m.stats().armorPercent())+"%"),y+48,LinkMod.CREAM);
                }
            }
            String message=note.isEmpty()?LinkMod.CLIENT.status():note;
            if(note.startsWith("Requesting invite") || note.startsWith("Ping queued") || note.startsWith("Clear queued"))
                message=LinkMod.CLIENT.actionStatus();
            line(g,message,top()+panelHeight()-48,LinkMod.ORANGE);
            line(g,page==2?"Ping sends this point even with XYZ sharing OFF":"Private key stays out of kits/HUD exports; no auto-connect",top()+panelHeight()-36,LinkMod.CREAM);
        }
        super.render(g,mx,my,partial);
    }
    private void line(GuiGraphics g,String s,int y,int color) { g.drawCenteredString(font,font.plainSubstrByWidth(s,panelWidth()-20),width/2,y,color); }
    private int panelWidth() { return Math.min(460,width-16); }
    private int panelHeight() { return Math.min(250,height-8); }
    private int left() { return (width-panelWidth())/2; }
    private int top() { return (height-panelHeight())/2; }
    @Override public void onClose() { if(!busy) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float tick) { }
}
