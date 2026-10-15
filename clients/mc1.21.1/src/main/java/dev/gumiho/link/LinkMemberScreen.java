package dev.gumiho.link;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import static dev.gumiho.link.LinkData.*;

/** Member actions are convenience controls; the relay independently authorizes every request. */
public final class LinkMemberScreen extends Screen {
    private static final List<String> ASSIGNABLE=List.of("COFOUNDER","INNER_CIRCLE","OFFICER","MEMBER");
    private final Screen parent;
    private int index;
    private String selectedId="", proposedRole="MEMBER", note="";
    private boolean busy;
    private String fingerprint="";
    public LinkMemberScreen(Screen parent) { super(Component.literal("Link Members | 링크 멤버")); this.parent=parent; }
    @Override protected void init() {
        int x=left()+10,w=panelWidth()-20,half=(w-4)/2,y=top()+34;
        if(width<320 || height<240) { button(width/2-50,height-28,100,"Done | 완료","",this::onClose); return; }
        List<Member> members=members(); Member target=selected(members); Identity self=LinkMod.CLIENT.identity();
        button(x,y,half,"< Previous | 이전","Browse team members.",() -> { index=Math.max(0,index-1); selectedId=""; rebuildWidgets(); });
        button(x+half+4,y,half,"Next | 다음 >","Browse team members.",() -> { index++; selectedId=""; rebuildWidgets(); });
        y+=72;
        if(target!=null && self!=null) {
            boolean hasCo=members.stream().anyMatch(member -> "COFOUNDER".equals(member.role()));
            if(!target.id().equals(selectedId)) { selectedId=target.id(); proposedRole=target.role(); }
            if(!"FOUNDER".equals(target.role()) && !target.id().equals(self.id())) {
                boolean managesRoles="FOUNDER".equals(self.role()) || "COFOUNDER".equals(self.role());
                if(managesRoles) {
                    button(x,y,w,"Proposed role: "+proposedRole+" | 변경 역할","Cycle a proposed role. Nothing changes until Apply Role is confirmed.",() -> {
                        proposedRole=ASSIGNABLE.get((ASSIGNABLE.indexOf(proposedRole)+1)%ASSIGNABLE.size()); rebuildWidgets();
                    });
                    y+=24;
                    if(!proposedRole.equals(target.role()) && LinkPolicy.canSetRole(self.id(),self.role(),target,proposedRole,
                            hasCo && !"COFOUNDER".equals(target.role())))
                        button(x,y,half,"Apply Role | 역할 적용","Confirmation required. Exact-role sharing is re-evaluated by the relay.",() -> confirmRole(target));
                    if(LinkPolicy.canRemove(self.id(),self.role(),target))
                        button(x+half+4,y,half,"Remove | 제거","Revokes this member key and clears their pings/live status. Confirmation required.",() -> confirmRemove(target));
                    y+=24;
                } else if(LinkPolicy.canRemove(self.id(),self.role(),target)) {
                    button(x,y,w,"Remove | 제거","Revokes this member key and clears their pings/live status. Confirmation required.",() -> confirmRemove(target));
                    y+=24;
                }
                if(LinkPolicy.canTransferFounder(self.id(),self.role(),target,hasCo)) {
                    button(x,y,w,"Transfer Founder | 창립자 이전","The target becomes Founder and you become Co-founder. Clears all invites/live status. Confirmation required.",() -> confirmTransfer(target));
                    y+=24;
                }
            }
            if(target.id().equals(self.id())) {
                button(x,y,w,"Rotate my member key | 내 키 교체","Immediately revokes your old key. The replacement is saved locally; confirmation required.",this::confirmRotate);
                y+=24;
            }
        }
        button(width/2-50,top()+panelHeight()-24,100,"Done | 완료","",this::onClose);
        fingerprint=fingerprint();
    }
    private List<Member> members() { return LinkMod.CLIENT.view().members(); }
    private Member selected(List<Member> members) {
        if(members.isEmpty()) return null;
        if(!selectedId.isEmpty()) for(int i=0;i<members.size();i++) if(members.get(i).id().equals(selectedId)) {index=i;return members.get(i);}
        index=Math.max(0,Math.min(index,members.size()-1)); return members.get(index);
    }
    private void confirmRole(Member target) {
        String role=proposedRole;
        minecraft.setScreen(new ConfirmScreen(yes -> { minecraft.setScreen(this); if(yes) {
            busy=true; note="Changing role | 역할 변경 중";
            LinkMod.CLIENT.setRole(target.id(),role,() -> success("Role changed | 역할 변경됨"),this::failure);
        }},Component.literal("Change "+target.name()+" to "+role+"?"),
                Component.literal("The relay enforces your role and immediately re-evaluates sharing permissions.")));
    }
    private void confirmRemove(Member target) {
        minecraft.setScreen(new ConfirmScreen(yes -> { minecraft.setScreen(this); if(yes) {
            busy=true; note="Removing member | 멤버 제거 중";
            LinkMod.CLIENT.removeMember(target.id(),() -> success("Member removed; their key is revoked"),this::failure);
        }},Component.literal("Remove "+target.name()+"?"),
                Component.literal("Their key, live state, pings and unused invitations are revoked. This cannot be undone here.")));
    }
    private void confirmTransfer(Member target) {
        minecraft.setScreen(new ConfirmScreen(yes -> { minecraft.setScreen(this); if(yes) {
            busy=true; note="Transferring Founder | 창립자 이전 중";
            LinkMod.CLIENT.transferFounder(target.id(),() -> success("Founder transferred; sessions are refreshing"),this::failure);
        }},Component.literal("Transfer Founder to "+target.name()+"?"),
                Component.literal("You become Co-founder. All invitations, pings and live status are cleared. This is a real authority change.")));
    }
    private void confirmRotate() {
        minecraft.setScreen(new ConfirmScreen(yes -> { minecraft.setScreen(this); if(yes) {
            busy=true; note="Rotating private key | 비공개 키 교체 중";
            LinkMod.CLIENT.rotateToken(this::storeRotatedToken,this::failure);
        }},Component.literal("Rotate your private Link key?"),
                Component.literal("The old key stops immediately. The replacement is saved only in gumiho-link-private.json.")));
    }
    private void storeRotatedToken(String token) {
        minecraft.execute(() -> {
            LinkMod.settings.memberToken=token;
            try {
                LinkMod.settings.save(LinkMod.PRIVATE_FILE);
                LinkMod.CLIENT.sample(LinkMod.capture(minecraft));
                LinkMod.CLIENT.connect(LinkMod.settings.endpoint,token,LinkMod.settings.sharing);
                busy=false; note="New private key saved; reconnecting | 새 키 저장됨";
            } catch(Exception ignored) {
                minecraft.keyboardHandler.setClipboard(token);
                LinkMod.CLIENT.disconnect(); busy=false;
                note="SAVE FAILED: new key copied to clipboard; store it privately before closing";
            }
            if(minecraft.screen==this) rebuildWidgets();
        });
    }
    private void success(String message) { minecraft.execute(() -> { busy=false;note=message;if(minecraft.screen==this)rebuildWidgets(); }); }
    private void failure(String message) { minecraft.execute(() -> { busy=false;note=message;if(minecraft.screen==this)rebuildWidgets(); }); }
    @Override public void tick() {
        super.tick();
        String next=fingerprint();
        if(!busy && !next.equals(fingerprint) && minecraft.screen==this) rebuildWidgets();
    }
    private String fingerprint() {
        Identity self=LinkMod.CLIENT.identity();
        StringBuilder value=new StringBuilder(self==null?"":self.id()+self.role());
        for(Member member:members()) value.append('|').append(member.id()).append(':').append(member.role());
        return value.toString();
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        super.renderBackground(g,mx,my,partial);
        g.fill(left(),top(),left()+panelWidth(),top()+panelHeight(),LinkMod.BACKGROUND);
        g.fill(left(),top(),left()+panelWidth(),top()+3,LinkMod.ORANGE);
        line(g,"Link Members | 링크 멤버",top()+10,LinkMod.CREAM);
        if(width<320 || height<240) line(g,"Lower GUI Scale to manage members",height/2,LinkMod.ORANGE);
        else {
            List<Member> members=members(); Member target=selected(members); Identity self=LinkMod.CLIENT.identity();
            int y=top()+62;
            if(target==null || self==null) line(g,"Connect to load the team roster | 연결 필요",y,LinkMod.CREAM);
            else {
                line(g,(index+1)+" / "+members.size()+" · "+target.name(),y,LinkMod.CREAM);
                line(g,target.role()+(target.id().equals(self.id())?" · YOU | 본인":"")+(target.online()?" · ONLINE":" · OFFLINE"),y+16,LinkMod.ORANGE);
                line(g,"Your role: "+self.role()+" | 내 역할",y+32,LinkMod.CREAM);
            }
            line(g,note.isEmpty()?LinkMod.CLIENT.actionStatus():note,top()+panelHeight()-47,LinkMod.ORANGE);
            line(g,"Role hints are UI only; the relay authorizes every action",top()+panelHeight()-35,LinkMod.CREAM);
        }
        super.render(g,mx,my,partial);
    }
    private void button(int x,int y,int w,String label,String help,Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(label,w-8)),ignored -> {if(!busy)action.run();})
                .bounds(x,y,w,18).tooltip(Tooltip.create(Component.literal(help.isEmpty()?label:label+"\n"+help))).build());
    }
    private void line(GuiGraphics g,String text,int y,int color) { g.drawCenteredString(font,font.plainSubstrByWidth(text,panelWidth()-20),width/2,y,color); }
    private int panelWidth() { return Math.min(460,width-16); }
    private int panelHeight() { return Math.min(250,height-8); }
    private int left() { return (width-panelWidth())/2; }
    private int top() { return (height-panelHeight())/2; }
    @Override public void onClose() { if(!busy)minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float tick) { }
}
