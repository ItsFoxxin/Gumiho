package dev.gumiho.link;

import com.google.gson.*;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

/** Bounded bodies/timeouts; no redirects, proxy credentials, cookies or secret-bearing logs. */
public final class LinkHttp implements AutoCloseable {
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER).proxy(ProxySelector.of(null)).build();
    public static final class Failure extends Exception {
        public final int status;
        public Failure(int status) { super("Link request failed ("+status+")"); this.status=status; }
    }
    public JsonObject request(URI endpoint, String token, String method, String path, JsonObject body) throws Exception {
        HttpRequest.Builder b=HttpRequest.newBuilder(endpoint.resolve(path)).timeout(Duration.ofSeconds(3))
                .header("Authorization","Bearer "+token).header("Accept","application/json");
        if(body==null) b.method(method,HttpRequest.BodyPublishers.noBody());
        else {
            byte[] bytes=LinkData.JSON.toJson(body).getBytes(StandardCharsets.UTF_8);
            if(bytes.length>16384) throw new Failure(413);
            b.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofByteArray(bytes));
        }
        CompletableFuture<HttpResponse<byte[]>> future=client.sendAsync(b.build(),info -> new LimitedBody());
        HttpResponse<byte[]> response;
        try { response=future.get(4,TimeUnit.SECONDS); }
        catch(Exception e) { future.cancel(true); throw e; }
        if(response.statusCode()<200 || response.statusCode()>=300) throw new Failure(response.statusCode());
        return JsonParser.parseString(new String(response.body(),StandardCharsets.UTF_8)).getAsJsonObject();
    }
    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> done=new CompletableFuture<>();
        private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        private Flow.Subscription subscription;
        public CompletionStage<byte[]> getBody() { return done; }
        public void onSubscribe(Flow.Subscription s) { subscription=s; s.request(1); }
        public void onNext(List<ByteBuffer> buffers) {
            for(ByteBuffer buffer:buffers) {
                if(buffer.remaining()>65536-bytes.size()) { subscription.cancel(); done.completeExceptionally(new Failure(413)); return; }
                byte[] part=new byte[buffer.remaining()]; buffer.get(part); bytes.writeBytes(part);
            }
            subscription.request(1);
        }
        public void onError(Throwable error) { done.completeExceptionally(error); }
        public void onComplete() { done.complete(bytes.toByteArray()); }
    }
    public void close() { client.shutdownNow(); }
}
