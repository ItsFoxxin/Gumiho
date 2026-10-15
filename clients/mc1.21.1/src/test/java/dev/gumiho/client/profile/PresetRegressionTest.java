package dev.gumiho.client.profile;

import com.google.gson.*;
import dev.gumiho.client.config.GumihoConfig;
import java.nio.file.*;
import java.util.List;

public final class PresetRegressionTest {
    private static int count;
    public static void main(String[] args) throws Exception {
        GumihoConfig original = new GumihoConfig();
        original.allowedServers = new java.util.ArrayList<>(List.of("private.example"));
        original.allowSingleplayer = false;
        original.playerGroups.put("private-player", "FRIEND");
        original.pinnedTarget = "private-target";
        original.meleeRange = 2.5;
        original.modules.put("auto_totem", true);
        original.hudX.put("brand_hud", 123);
        original.hudY.put("brand_hud", 45);
        original.hudScales.put("brand_hud",.7);
        String kit = PresetCodec.encode(original,PresetCodec.Kind.KIT,"26.2");
        check(!kit.contains("private"),"no private values exported");
        for (String excluded : List.of("allowedServers","allowSingleplayer","playerGroups","pinnedTarget","schemaVersion","activeProfile"))
            check(!kit.contains("\"" + excluded + "\""),"private field excluded");
        GumihoConfig loaded = PresetCodec.merge(kit,PresetCodec.Kind.KIT,new GumihoConfig());
        check(loaded.meleeRange==2.5 && loaded.modules.get("auto_totem"),"kit settings and toggles");
        check(loaded.hudX.get("brand_hud")==123 && loaded.hudY.get("brand_hud")==45,"kit layout");
        check(loaded.allowedServers.contains("localhost"),"destination permissions retained");
        GumihoConfig copy = PresetCodec.copy(original);
        copy.hudX.put("brand_hud",900);
        check(original.hudX.get("brand_hud")==123,"deep snapshot");
        String hud = PresetCodec.encode(original,PresetCodec.Kind.HUD,"26.2");
        check(!hud.contains("modules") && !hud.contains("meleeRange"),"HUD only");
        GumihoConfig destination = new GumihoConfig();
        destination.meleeRange=4;
        destination.modules.put("auto_totem",false);
        loaded=PresetCodec.merge(hud,PresetCodec.Kind.HUD,destination);
        check(loaded.meleeRange==4 && !loaded.modules.get("auto_totem"),"HUD never changes settings or toggles");
        check(loaded.hudX.get("brand_hud")==123,"cross-version HUD");
        check(loaded.hudScales.get("brand_hud")==.7,"cross-version panel size");
        check(destination.hudX.isEmpty(),"merge does not mutate input");
        JsonObject malicious=JsonParser.parseString(kit).getAsJsonObject();
        JsonObject settings=malicious.getAsJsonObject("settings");
        settings.addProperty("allowSingleplayer",true);
        settings.add("allowedServers",JsonParser.parseString("[\"attacker.example\"]"));
        settings.addProperty("pinnedTarget","someone");
        settings.add("playerGroups",JsonParser.parseString("{\"someone\":\"ENEMY\"}"));
        settings.addProperty("futureUnsupportedSetting",true);
        loaded=PresetCodec.merge(malicious.toString(),PresetCodec.Kind.KIT,original);
        check(!loaded.allowSingleplayer && loaded.allowedServers.equals(original.allowedServers),"permission injection ignored");
        check(loaded.playerGroups.equals(original.playerGroups) && loaded.pinnedTarget.equals(original.pinnedTarget),"identity injection ignored");
        for(String invalid : List.of("", "{}", "[]", "null", hud.replace("\"version\": 1","\"version\": 2"),
                hud.replace("\"HUD\"","\"KIT\""), hud.replace("123","1.5"), hud.replace("123","null"),
                hud.replace("123","100001"), hud.replace("123","\"wrong\""), " ".repeat(65537))) {
            rejects(() -> PresetCodec.merge(invalid,PresetCodec.Kind.HUD,original),"invalid HUD rejected");
        }
        JsonObject malformed=JsonParser.parseString(hud).getAsJsonObject();
        malformed.getAsJsonObject("settings").remove("hudY");
        rejects(() -> PresetCodec.merge(malformed.toString(),PresetCodec.Kind.HUD,original),"missing axis rejected");
        malformed.getAsJsonObject("settings").add("hudY",new JsonObject());
        rejects(() -> PresetCodec.merge(malformed.toString(),PresetCodec.Kind.HUD,original),"mismatched axes rejected");
        for(String value: List.of("null","\"true\"","4","[]")) {
            JsonObject wrong=JsonParser.parseString(kit).getAsJsonObject();
            wrong.getAsJsonObject("settings").getAsJsonObject("modules").add("auto_totem",JsonParser.parseString(value));
            rejects(() -> PresetCodec.merge(wrong.toString(),PresetCodec.Kind.KIT,original),"invalid toggle");
        }
        for(String invalid: List.of("","../outside","..","C:\\file","a/b","a\\b","CON","nul","COM1"," LPT","test ","a".repeat(41)))
            rejects(() -> PresetStore.validName(invalid),"unsafe filename");
        for(String valid: List.of("Bow kit","Mace-2","구미호","Tower_1"))
            check(PresetStore.validName(valid).equals(valid),"valid filename");
        for(String value:List.of("null","0","2",new JsonPrimitive("small").toString(),new JsonPrimitive("0.5").toString())) {
            JsonObject badScale=JsonParser.parseString(hud).getAsJsonObject();
            badScale.getAsJsonObject("settings").getAsJsonObject("hudScales").add("brand_hud",JsonParser.parseString(value));
            rejects(() -> PresetCodec.merge(badScale.toString(),PresetCodec.Kind.HUD,original),"invalid scale");
        }
        Path folder=Files.createTempDirectory("gumiho-preset-test-");
        PresetStore store=new PresetStore(folder);
        try {
            store.save("Bow",PresetCodec.Kind.KIT,kit);
            store.save("Bow",PresetCodec.Kind.HUD,hud);
            store.save("구미호",PresetCodec.Kind.KIT,kit);
            check(store.read("Bow",PresetCodec.Kind.KIT).equals(kit),"disk kit round trip");
            check(store.read("Bow",PresetCodec.Kind.HUD).equals(hud),"disk HUD round trip");
            check(store.names(PresetCodec.Kind.KIT).size()==2,"browse kits");
            check(store.names(PresetCodec.Kind.HUD).size()==1,"separate HUD namespace");
            boolean collision=false;
            try { store.save("Bow",PresetCodec.Kind.KIT,"changed"); }
            catch (FileAlreadyExistsException expected) { collision=true; }
            check(collision && store.read("Bow",PresetCodec.Kind.KIT).equals(kit),"save never overwrites");
            boolean missing=false;
            try { store.read("Missing",PresetCodec.Kind.KIT); } catch (java.io.IOException expected) { missing=true; }
            check(missing,"missing file error");
        } finally {
            Files.deleteIfExists(folder.resolve("kit/Bow.json"));
            Files.deleteIfExists(folder.resolve("kit/구미호.json"));
            Files.deleteIfExists(folder.resolve("hud/Bow.json"));
            Files.deleteIfExists(folder.resolve("kit"));
            Files.deleteIfExists(folder.resolve("hud"));
            Files.deleteIfExists(folder);
        }
        System.out.println("Preset regression: " + count + " assertions passed");
    }
    private static void check(boolean result,String label) { count++; if(!result) throw new AssertionError(label); }
    private static void rejects(Runnable action,String label) {
        boolean rejected=false;
        try { action.run(); } catch(IllegalArgumentException expected) { rejected=true; }
        check(rejected,label);
    }
}
