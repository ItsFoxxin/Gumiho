package dev.gumiho.client.module;

import dev.gumiho.client.config.GumihoConfigManager;
import dev.gumiho.client.security.ServerPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class StartupRegressionTest {
    private static int checks;
    public static void main(String[] args) {
        var config=new GumihoConfigManager();
        var policy=new ServerPolicy(config);
        var manager=new ModuleManager(config,policy);
        var guard=new Counting("guard",false,true);
        var off=new Counting("off",true,false);
        var visual=new Counting("visual",false,false);
        var defaults=new Counting("defaults",true,false);
        config.config().modules.put("guard",true);
        config.config().modules.put("off",false);
        config.config().modules.put("visual",true);
        config.config().hudX.put("hud",123);
        config.config().hudY.put("hud",45);
        manager.register(guard); manager.register(off); manager.register(visual); manager.register(defaults);
        var hud=new HudModule(); manager.register(hud);
        check(guard.enabled() && visual.enabled() && !off.enabled(),"restore intent");
        check(guard.enables==0 && visual.enables==0 && off.disables==0,"no constructor hooks");
        check(hud.preferredX()==123 && hud.preferredY()==45,"saved positions preserved");
        check(!policy.canUseExperimentalModules(null),"null policy denies");
        manager.tick(null); manager.frame(null,0);
        check(guard.enables==0 && visual.enables==0,"null client deferred");
        var client=new Minecraft();
        check(!guard.setEnabled(true,null,policy),"null manual toggle denied");
        manager.tick(client);
        check(visual.enables==0,"options/font deferred");
        client.options=new Object(); manager.tick(client);
        check(visual.enables==0,"font deferred");
        client.font=new Object(); client.server=new ServerData("localhost");
        check(!policy.canUseExperimentalModules(client),"no player or level denied");
        manager.tick(client);
        check(visual.enables==1 && defaults.enables==1 && guard.enables==0,"visual runtime activation only");
        client.player=new Object();
        check(!policy.canUseExperimentalModules(client),"no level denied");
        client.level=new Object(); client.server=new ServerData("unlisted.example");
        manager.tick(client);
        check(guard.enables==0 && guard.ticks==0,"unlisted server not activated");
        client.server=new ServerData(" LOCALHOST ");
        manager.tick(client); manager.tick(client); manager.frame(client,0);
        check(guard.enables==1 && guard.ticks==2 && guard.frames==1,"one authorized enable");
        check(visual.enables==1 && defaults.enables==1,"visual hooks not repeated");
        client.server=new ServerData("unlisted.example");
        manager.tick(client); manager.frame(client,0);
        check(guard.ticks==2 && guard.frames==1 && guard.suspends>0,"suspended outside allowed server");
        check(guard.enabled() && config.config().modules.get("guard"),"saved intent not erased");
        check(!guard.setEnabled(true,client,policy),"manual enable still denied");
        check(guard.setEnabled(false,client,policy) && guard.disables==1,"disable remains available");
        client.server=new ServerData("localhost");
        manager.tick(client);
        check(!guard.enabled() && guard.ticks==2,"disabled intent stays off");
        check(guard.setEnabled(true,client,policy) && guard.enables==2,"explicit re-enable");
        manager.tick(client);
        check(guard.enables==2 && guard.ticks==3,"no duplicate enable hook");
        client.server=null; client.singleplayer=true; config.config().allowSingleplayer=false;
        check(!policy.canUseExperimentalModules(client),"single-player opt out");
        config.config().allowSingleplayer=true;
        check(policy.canUseExperimentalModules(client),"authorized single-player");
        client.singleplayer=false; client.server=new ServerData(null);
        check(!policy.canUseExperimentalModules(client),"null address");
        client.server=new ServerData(""); config.config().allowedServers.add(null);
        check(!policy.canUseExperimentalModules(client),"blank and null entries cannot grant permission");
        hud.setScale(.7);
        manager.renderHud(null,client);
        check(hud.renders==1,"gameplay HUD renders once");
        client.screen=new dev.gumiho.client.gui.HudEditorScreen();
        manager.renderHud(null,client);
        check(hud.renders==1,"editor suppresses underlying gameplay HUD");
        hud.renderScaled(null,client);
        check(hud.renders==2,"enabled editor preview renders once");
        hud.setEnabled(false,client,policy);
        manager.renderHud(null,client);
        hud.renderScaled(null,client);
        check(hud.renders==3,"disabled preview has same single-pass count");
        check(hud.preferredX()==123 && hud.preferredY()==45 && hud.scale()==.7,
                "render ownership never changes saved positions or size");
        client.screen=null;
        manager.renderHud(null,client);
        check(hud.renders==3,"disabled HUD stays hidden after editor closes");
        hud.setEnabled(true,client,policy);
        manager.renderHud(null,client);
        check(hud.renders==4,"gameplay HUD resumes after editor closes");
        System.out.println("Startup lifecycle/render ownership regression: "+checks+" assertions passed");
    }
    private static final class Counting extends GumihoModule {
        int enables,disables,ticks,frames,suspends;
        Counting(String id,boolean enabled,boolean guarded) {super(id,id,id,"","",ModuleCategory.COMBAT,enabled,guarded);}
        @Override protected void onEnable(Minecraft client) {
            if(!runtimeReady(client)) throw new AssertionError("Early onEnable");
            enables++;
        }
        @Override protected void onDisable(Minecraft client) {disables++;}
        @Override public void tick(Minecraft client) {ticks++;}
        @Override public void frame(Minecraft client,float partial) {frames++;}
        @Override public void suspend(Minecraft client) {suspends++;}
    }
    private static void check(boolean result,String label) {checks++;if(!result)throw new AssertionError(label);}
}
