package dev.gumiho.link;

import com.google.gson.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import static dev.gumiho.link.LinkData.*;

/** All HTTP is serialized on one daemon worker. The game thread supplies ONLY its own sample. */
public final class LinkClient implements AutoCloseable {
    private record Intent(URI endpoint,String token,Sharing sharing,Context context,long revision) {
        @Override public String toString() { return "LinkIntent[redacted]"; }
    }
    private record Command(Intent owner,String method,String path,JsonObject body,boolean refreshSession,
                           Consumer<JsonObject> done,Consumer<String> failed) { }
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t=new Thread(r,"GUMIHO-Link"); t.setDaemon(true); return t;
    });
    private final ArrayBlockingQueue<Command> commands=new ArrayBlockingQueue<>(8);
    private volatile Intent desired;
    private volatile Sample sample=Sample.empty();
    private volatile long sampledAt;
    private volatile View view=View.empty();
    private volatile Identity identity;
    private volatile String actionStatus="";
    private volatile String status="Disconnected | 연결 안 됨";
    private long revision;
    private boolean started;
    // Worker-only state; never read by Minecraft rendering.
    private LinkHttp http;
    private Intent active;
    private String epoch,team;
    private long sequence;
    private int failures;
    private long retryAt;
    public synchronized void connect(String endpoint,String token,Sharing sharing) {
        desired=new Intent(LinkData.endpoint(endpoint),LinkData.token(token,"gl1_"),sharing,sample.context(),++revision);
        clear(); status="Connecting | 연결 중"; start();
    }
    public synchronized void disconnect() {
        desired=null; ++revision; clear(); status="Disconnected | 연결 안 됨";
    }
    public synchronized void sample(Sample next) {
        sample=next; sampledAt=System.nanoTime();
        Intent d=desired;
        if(d!=null && !Objects.equals(d.context,next.context())) {
            desired=new Intent(d.endpoint,d.token,d.sharing,next.context(),++revision);
            clear(); status="World changed; refreshing | 갱신 중";
        }
    }
    public boolean requested() { return desired!=null; }
    public String status() { return status; }
    public String actionStatus() { return actionStatus; }
    public Identity identity() { return identity; }
    public View view() { return view.fresh(System.nanoTime()); }
    private void clear() { view=View.empty(); identity=null; commands.clear(); }
    private void start() { if(!started) { started=true; worker.scheduleWithFixedDelay(this::cycle,0,1000,TimeUnit.MILLISECONDS); } }
    private JsonObject call(Intent d,String method,String path,JsonObject body) throws Exception {
        if(http==null) http=new LinkHttp();
        return http.request(d.endpoint,d.token,method,path,body);
    }
    private JsonObject sequenceBody() {
        JsonObject b=new JsonObject(); b.addProperty("epoch",epoch); b.addProperty("sequence",sequence++); return b;
    }
    private void retire() {
        if(active!=null && epoch!=null) {
            try { call(active,"POST","/v1/me/disconnect",sequenceBody()); } catch(Exception ignored) { }
        }
        active=null; epoch=null;
    }
    private void cycle() {
        Intent d=desired;
        try {
            if(d!=active) { retire(); active=d; failures=0; retryAt=0; }
            if(d==null || System.nanoTime()<retryAt) return;
            if(System.nanoTime()-sampledAt>2000000000L) {
                retire(); synchronized(this) { if(desired==d) { clear(); status="Game data paused; sharing suspended"; } } return;
            }
            if(epoch==null) {
                JsonObject s=call(d,"GET","/v1/session",null);
                if(s.get("protocol").getAsInt()!=1 || s.get("liveTtlMs").getAsInt()!=5000) throw new IllegalArgumentException();
                epoch=s.get("epoch").getAsString(); UUID.fromString(epoch);
                sequence=s.get("nextSequence").getAsLong();
                if(sequence<1 || sequence>9007199254740000L) throw new IllegalArgumentException();
                team=text(s.getAsJsonObject("team"),"name");
                identity=LinkData.identity(s.getAsJsonObject("self"));
            }
            Sample own=sample;
            if(d!=desired || !Objects.equals(d.context,own.context())) return;
            JsonObject b=sequenceBody(); b.add("context",JSON.toJsonTree(d.context)); b.add("sharing",JSON.toJsonTree(d.sharing));
            if(d.context!=null) {
                if(!d.sharing.locationRoles().isEmpty()) b.add("location",JSON.toJsonTree(own.location()));
                if(!d.sharing.statsRoles().isEmpty()) b.add("stats",JSON.toJsonTree(own.stats()));
            }
            call(d,"PUT","/v1/me/status",b);
            if(d!=desired) return;
            Command command=commands.poll();
            if(command!=null && command.owner==d) {
                JsonObject body=command.body;
                if(command.path.equals("/v1/pings")) { body.addProperty("epoch",epoch); body.add("context",JSON.toJsonTree(d.context)); }
                JsonObject answer;
                try { answer=call(d,command.method,command.path,body); }
                catch(Exception error) {
                    synchronized(this) { if(desired==d) {
                        actionStatus=error instanceof LinkHttp.Failure f
                                ? "Action rejected ("+f.status+"); check role/context" : "Action failed; check connection";
                        command.failed.accept(actionStatus);
                    } }
                    throw error;
                }
                synchronized(this) { if(desired==d) { actionStatus="Action completed | 완료"; command.done.accept(answer); } }
                if(command.refreshSession) { epoch=null; view=View.empty(); identity=null; return; }
            }
            long start=System.nanoTime();
            View next=snapshot(call(d,"GET","/v1/snapshot",null),epoch,team,start);
            synchronized(this) {
                if(desired==d) { view=next; status=d.context==null ? "Connected; no multiplayer world | 연결됨" : "Connected | 연결됨"; failures=0; }
            }
        } catch(Exception e) {
            synchronized(this) {
                if(desired!=d) return;
                clear();
                if(e instanceof LinkHttp.Failure f && f.status==401) { desired=null; status="Credential rejected; reconnect | 인증 실패"; }
                else {
                    int seconds=Math.min(30,1<<Math.min(5,failures++));
                    retryAt=System.nanoTime()+seconds*1000000000L;
                    status="Link unavailable; data cleared | 연결 확인";
                    if(e instanceof LinkHttp.Failure f && f.status==409) epoch=null;
                }
            }
        }
    }
    public synchronized boolean ping(Position position,String kind,List<String> audience) {
        if(desired==null || desired.context==null || !KINDS.contains(kind) || audience.isEmpty()) return false;
        JsonObject b=new JsonObject(); b.add("position",JSON.toJsonTree(position)); b.addProperty("kind",kind);
        b.add("audienceRoles",JSON.toJsonTree(roles(audience)));
        actionStatus="Ping queued | 핑 대기";
        return commands.offer(new Command(desired,"POST","/v1/pings",b,false,ignored -> { },ignored -> { }));
    }
    public synchronized boolean clearPings() {
        actionStatus="Clear queued | 삭제 대기";
        return desired!=null && commands.offer(new Command(desired,"DELETE","/v1/me/pings",null,false,
                ignored -> { },ignored -> { }));
    }
    public synchronized boolean invite(Consumer<String> result) {
        actionStatus="Requesting invite | 초대 요청";
        return desired!=null && commands.offer(new Command(desired,"POST","/v1/invites",new JsonObject(),false,
                answer -> result.accept(token(answer.get("token").getAsString(),"gli_")),ignored -> { }));
    }
    private synchronized boolean manage(String method,String path,JsonObject body,boolean refresh,
                                        Consumer<JsonObject> success,Consumer<String> failure) {
        if(desired==null) { failure.accept("Connect first | 먼저 연결하세요"); return false; }
        actionStatus="Management action queued | 관리 작업 대기";
        boolean queued=commands.offer(new Command(desired,method,path,body,refresh,success,failure));
        if(!queued) { actionStatus="Link is busy; try again | 잠시 후 다시 시도"; failure.accept(actionStatus); }
        return queued;
    }
    public boolean setRole(String memberId,String role,Runnable success,Consumer<String> failure) {
        JsonObject body=new JsonObject(); body.addProperty("role",role);
        return manage("PUT","/v1/members/"+memberId+"/role",body,false,ignored -> success.run(),failure);
    }
    public boolean removeMember(String memberId,Runnable success,Consumer<String> failure) {
        return manage("DELETE","/v1/members/"+memberId,null,false,ignored -> success.run(),failure);
    }
    public boolean rotateToken(Consumer<String> success,Consumer<String> failure) {
        return manage("POST","/v1/me/token",new JsonObject(),false,
                answer -> success.accept(token(answer.get("token").getAsString(),"gl1_")),failure);
    }
    public boolean transferFounder(String memberId,Runnable success,Consumer<String> failure) {
        JsonObject body=new JsonObject(); body.addProperty("memberId",memberId);
        body.addProperty("confirmation","TRANSFER_FOUNDER");
        return manage("PUT","/v1/team/founder",body,true,ignored -> success.run(),failure);
    }
    /** Provisioning is explicit, returns a member credential, and NEVER connects or shares automatically. */
    public synchronized CompletableFuture<String> provision(String endpoint,String credential,String displayName,String teamName) {
        disconnect(); long requestRevision=revision; start();
        URI uri=LinkData.endpoint(endpoint);
        boolean create=credential.startsWith("glb_"); token(credential,create?"glb_":"gli_");
        CompletableFuture<String> result=new CompletableFuture<>();
        worker.execute(() -> {
            try {
                retire(); JsonObject b=new JsonObject(); b.addProperty("displayName",displayName);
                if(create) b.addProperty("teamName",teamName);
                if(http==null) http=new LinkHttp();
                String value=token(http.request(uri,credential,"POST",create?"/v1/teams":"/v1/join",b).get("token").getAsString(),"gl1_");
                synchronized(this) { if(revision==requestRevision) result.complete(value); else result.completeExceptionally(new IllegalStateException("Cancelled")); }
            } catch(Exception ignored) { result.completeExceptionally(new IllegalStateException("Join/create failed; check relay and credential")); }
        });
        return result;
    }
    public void close() {
        disconnect(); worker.execute(() -> { retire(); if(http!=null) http.close(); }); worker.shutdown();
    }
}
