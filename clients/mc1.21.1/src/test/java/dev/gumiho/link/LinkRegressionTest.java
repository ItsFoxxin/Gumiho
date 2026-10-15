package dev.gumiho.link;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import java.util.function.BooleanSupplier;
import static dev.gumiho.link.LinkData.*;

public final class LinkRegressionTest {
    private static int checks;
    private static void check(boolean yes,String why) { checks++; if(!yes) throw new AssertionError(why); }
    private static void reject(Runnable run) { try { run.run(); throw new AssertionError("Expected rejection"); } catch(IllegalArgumentException expected) { checks++; } }
    private static JsonObject json(String value) { return JsonParser.parseString(value).getAsJsonObject(); }
    private static void await(BooleanSupplier ready,String why) throws Exception {
        long end=System.nanoTime()+12000000000L;
        while(!ready.getAsBoolean() && System.nanoTime()<end) Thread.sleep(50);
        check(ready.getAsBoolean(),why);
    }
    public static void main(String[] args) throws Exception {
        for(String invalid:List.of("http://example.com","http://localhost:8791","http://127.0.0.2:8791","https://a/b","https://a?token=x","https://u:p@a","ftp://a","https://a#x","http://127.0.0.1:0")) reject(() -> endpoint(invalid));
        check(endpoint("http://127.0.0.1:8791/").toString().equals("http://127.0.0.1:8791"),"loopback normalization");
        check(endpoint("https://relay.example").getScheme().equals("https"),"TLS endpoint");
        check(serverKey("PLAY.Example:25565 ").equals(serverKey("play.example")),"default port normalized");
        check(!serverKey("play.example:25566").equals(serverKey("play.example")),"custom port separate");
        reject(() -> new Sharing(List.of("MEMBER","MEMBER"),List.of()));
        reject(() -> new Sharing(List.of("EVERYONE"),List.of()));
        check(Sharing.none().locationRoles().isEmpty(),"sharing off by default");
        Path dir=Files.createTempDirectory("gumiho-link-regression-");
        try {
            Path path=dir.resolve("private.json"); LinkSettings s=new LinkSettings(); s.save(path);
            check(LinkSettings.load(path).memberToken.isEmpty(),"no credential by default");
            check(!Files.readString(path).contains("location\""),"no snapshots in settings");
            Files.writeString(path,"broken");
            try { LinkSettings.load(path); throw new AssertionError(); } catch(IOException expected) { checks++; }
            check(Files.readString(path).equals("broken"),"corrupt config preserved");
            Files.delete(path);
        } finally { Files.delete(dir); }
        View cached=new View("Team",List.of(new Member("id","Name","MEMBER",true,new Position(1,2,3),null,10)),
                List.of(new Ping("p","id","HELP",new Position(1,2,3),10)));
        check(cached.fresh(11).members().getFirst().location()==null && !cached.fresh(11).members().getFirst().online(),"local expiry without network");
        check(cached.fresh(11).pings().isEmpty(),"local ping expiry");
        LinkData.Member member=new LinkData.Member("00000000-0000-0000-0000-000000000002","Member","MEMBER",true,null,null,20);
        LinkData.Member officer=new LinkData.Member("00000000-0000-0000-0000-000000000003","Officer","OFFICER",true,null,null,20);
        check(LinkPolicy.canInvite("OFFICER") && !LinkPolicy.canInvite("MEMBER"),"invite hints match roles");
        check(LinkPolicy.canSetRole("owner","FOUNDER",member,"COFOUNDER",false),"Founder can appoint Co-founder");
        check(!LinkPolicy.canSetRole("co","COFOUNDER",member,"COFOUNDER",false),"Co-founder cannot appoint peer");
        check(LinkPolicy.canSetRole("co","COFOUNDER",member,"INNER_CIRCLE",false),"Co-founder can promote Member");
        check(LinkPolicy.canRemove("officer","OFFICER",member),"Officer can remove Member");
        check(!LinkPolicy.canRemove("officer","OFFICER",officer),"Officer cannot remove Officer");
        check(LinkPolicy.canTransferFounder("owner","FOUNDER",member,false),"Founder can transfer without Co-founder");
        check(!LinkPolicy.canTransferFounder("owner","FOUNDER",member,true),"existing Co-founder is required transfer target");
        httpLimits();
        if(args.length==2) integration(args[0],args[1]);
        System.out.println("Link regression: "+checks+" checks passed"+(args.length==2?" including real Java/Node exchange":" (pure checks; integration not requested)"));
    }
    private static void httpLimits() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        AtomicBoolean redirected=new AtomicBoolean();
        var executor=Executors.newVirtualThreadPerTaskExecutor(); server.setExecutor(executor);
        server.createContext("/large",e -> {
            byte[] bytes=new byte[65537]; e.sendResponseHeaders(200,bytes.length);
            try { e.getResponseBody().write(bytes); } catch(IOException ignored) { } finally {e.close();}
        });
        server.createContext("/redirect",e -> {e.getResponseHeaders().add("Location","/target");e.sendResponseHeaders(302,-1);e.close();});
        server.createContext("/target",e -> {redirected.set(true);e.sendResponseHeaders(200,-1);e.close();});
        server.start();
        try(LinkHttp http=new LinkHttp()) {
            var uri=endpoint("http://127.0.0.1:"+server.getAddress().getPort());
            for(String route:List.of("/large","/redirect")) {
                boolean failed=false;
                try { http.request(uri,"gl1_"+"x".repeat(43),"GET",route,null); } catch(Exception expected){failed=true;}
                check(failed,route+" rejected");
            }
            check(!redirected.get(),"credentials never follow redirect");
        } finally {server.stop(0);executor.close();}
    }
    private static Member member(LinkClient client,String name) { return client.view().members().stream().filter(m -> m.name().equals(name)).findFirst().orElse(null); }
    private static void integration(String node,String fixture) throws Exception {
        Process relay=new ProcessBuilder(node,fixture).start();
        var reader=new BufferedReader(new InputStreamReader(relay.getInputStream()));
        // Fixture credentials are never printed or persisted.
        String line=CompletableFuture.supplyAsync(() -> { try {return reader.readLine();} catch(IOException e){throw new CompletionException(e);} }).get(8,TimeUnit.SECONDS);
        JsonObject info=json(line); var url=endpoint(info.get("endpoint").getAsString());
        ScheduledExecutorService feed=Executors.newSingleThreadScheduledExecutor();
        try(LinkHttp http=new LinkHttp(); LinkClient alice=new LinkClient(); LinkClient bob=new LinkClient()) {
            String a=alice.provision(url.toString(),info.get("bootstrap").getAsString(),"Alice","FoxTest").get(8,TimeUnit.SECONDS);
            check(!alice.requested(),"owner provisioning does not connect");
            String invite=http.request(url,a,"POST","/v1/invites",json("{}")).get("token").getAsString();
            String b=bob.provision(url.toString(),invite,"Bob","").get(8,TimeUnit.SECONDS);
            check(!bob.requested(),"invite provisioning does not connect");
            String bid=http.request(url,b,"GET","/v1/session",null).getAsJsonObject("self").get("id").getAsString();
            Context context=new Context(serverKey("play.test"),"minecraft:overworld");
            Sample own=new Sample(context,new Position(1,64,2),new Stats(12,20,18,75.0,"minecraft:bow","minecraft:shield"));
            AtomicBoolean feedAlice=new AtomicBoolean(true);
            AtomicReference<Sample> aliceSample=new AtomicReference<>(own);
            feed.scheduleAtFixedRate(() -> { if(feedAlice.get()) alice.sample(aliceSample.get()); bob.sample(own); },0,100,TimeUnit.MILLISECONDS);
            alice.sample(own); bob.sample(own);
            alice.connect(url.toString(),a,Sharing.none()); bob.connect(url.toString(),b,Sharing.none());
            await(() -> member(bob,"Alice")!=null && member(bob,"Alice").online(),"two live clients");
            check(member(bob,"Alice").location()==null && member(bob,"Alice").stats()==null,"default privacy");
            alice.connect(url.toString(),a,new Sharing(List.of("MEMBER"),List.of()));
            await(() -> member(bob,"Alice")!=null && member(bob,"Alice").location()!=null,"exact role location visibility");
            check(member(bob,"Alice").stats()==null,"stats withheld independently");
            check(alice.ping(new Position(8,64,9),"HELP",List.of("MEMBER")),"explicit ping queued");
            await(() -> !bob.view().pings().isEmpty(),"teammate receives ping");
            http.request(url,a,"PUT","/v1/members/"+bid+"/role",json("{\"role\":\"INNER_CIRCLE\"}"));
            await(() -> member(bob,"Alice")!=null && member(bob,"Alice").location()==null && bob.view().pings().isEmpty(),"role change removes location and ping");
            alice.connect(url.toString(),a,new Sharing(List.of("INNER_CIRCLE"),List.of("INNER_CIRCLE")));
            await(() -> member(bob,"Alice")!=null && member(bob,"Alice").stats()!=null,"both categories independently consented");
            aliceSample.set(new Sample(new Context(context.serverKey(),"minecraft:the_nether"),own.location(),own.stats()));
            alice.sample(aliceSample.get());
            check(alice.view().members().isEmpty(),"world transition clears local cache immediately");
            await(() -> member(bob,"Alice")!=null && member(bob,"Alice").location()==null,"dimension filter clears peer fields");
            aliceSample.set(own); alice.sample(own);
            await(() -> member(bob,"Alice")!=null && member(bob,"Alice").location()!=null,"same context resumes");
            feedAlice.set(false);
            await(() -> member(bob,"Alice")!=null && !member(bob,"Alice").online(),"stalled game feed stops sharing");
            feedAlice.set(true);
            await(() -> member(bob,"Alice")!=null && member(bob,"Alice").location()!=null,"fresh feed resumes");
            alice.disconnect(); check(alice.view().members().isEmpty(),"local disconnect clears immediately");
            await(() -> member(bob,"Alice")!=null && !member(bob,"Alice").online(),"relay disconnect clears peer state");
            http.request(url,a,"DELETE","/v1/members/"+bid,null);
            await(() -> !bob.requested() && bob.view().members().isEmpty(),"revoked credential disconnects and clears");
        } finally {
            feed.shutdownNow(); relay.getOutputStream().close();
            if(!relay.waitFor(5,TimeUnit.SECONDS)) relay.destroyForcibly();
        }
    }
}
