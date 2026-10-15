package dev.gumiho.link;

import com.google.gson.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Wire data only. No Minecraft, combat modules, inventory exports or remote commands. */
public final class LinkData {
    public static final List<String> ROLES = List.of("FOUNDER", "COFOUNDER", "INNER_CIRCLE", "OFFICER", "MEMBER");
    public static final List<String> KINDS = List.of("LOCATION", "DANGER", "HELP", "REGROUP");
    public static final Gson JSON = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    public record Sharing(List<String> locationRoles, List<String> statsRoles) {
        public Sharing { locationRoles = roles(locationRoles); statsRoles = roles(statsRoles); }
        public static Sharing none() { return new Sharing(List.of(), List.of()); }
    }
    public record Context(String serverKey, String dimension) { }
    public record Position(double x, double y, double z) { }
    public record Stats(double health, double maxHealth, int hunger, Double armorPercent, String mainItem, String offItem) { }
    public record Sample(Context context, Position location, Stats stats) {
        public static Sample empty() { return new Sample(null, null, null); }
    }
    public record Member(String id, String name, String role, boolean online, Position location, Stats stats, long deadline) { }
    public record Identity(String id, String name, String role) { }
    public record Ping(String id, String senderId, String kind, Position position, long deadline) { }
    public record View(String team, List<Member> members, List<Ping> pings) {
        public View { members = List.copyOf(members); pings = List.copyOf(pings); }
        public static View empty() { return new View("", List.of(), List.of()); }
        public View fresh(long now) {
            return new View(team, members.stream().map(m -> m.deadline > now ? m :
                    new Member(m.id,m.name,m.role,false,null,null,0)).toList(),
                    pings.stream().filter(p -> p.deadline > now).toList());
        }
    }
    public static URI endpoint(String raw) {
        URI u = URI.create(raw.trim());
        String host = u.getHost();
        boolean loopback = "127.0.0.1".equals(host) || "[::1]".equals(host);
        if (host == null || !("https".equals(u.getScheme()) || ("http".equals(u.getScheme()) && loopback))
                || u.getRawUserInfo() != null || u.getRawQuery() != null || u.getRawFragment() != null
                || !(u.getPath().isEmpty() || u.getPath().equals("/")) || u.getPort() == 0)
            throw new IllegalArgumentException("Use HTTPS, or HTTP with literal 127.0.0.1 for local tests; no path or credentials.");
        return URI.create(u.getScheme()+"://"+u.getRawAuthority());
    }
    public static String token(String token, String prefix) {
        if (token == null || !token.matches(prefix + "[A-Za-z0-9_-]{43}"))
            throw new IllegalArgumentException("Invalid Link credential format");
        return token;
    }
    public static List<String> roles(List<String> values) {
        if (values == null || values.size() > 5 || !ROLES.containsAll(values) || new HashSet<>(values).size() != values.size())
            throw new IllegalArgumentException("Invalid audience");
        return List.copyOf(values);
    }
    public static String serverKey(String address) {
        String s = address.trim().toLowerCase(Locale.ROOT);
        if (s.endsWith(":25565")) s = s.substring(0,s.length()-6);
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
    public static String text(JsonObject o, String key) {
        String s = o.get(key).getAsString();
        if (s.length() > 128) throw new IllegalArgumentException("Oversized display field");
        return s.replaceAll("[\\p{Cc}\\p{Cf}§]", "");
    }
    public static String id(JsonObject o, String key) {
        String value=text(o,key);
        try { UUID.fromString(value); return value; }
        catch(IllegalArgumentException error) { throw new IllegalArgumentException("Invalid member id"); }
    }
    public static Identity identity(JsonObject o) {
        String role=text(o,"role");
        if(!ROLES.contains(role)) throw new IllegalArgumentException("Invalid role");
        return new Identity(id(o,"id"),text(o,"name"),role);
    }
    private static double number(JsonObject o, String k, double min, double max) {
        double n = o.get(k).getAsDouble();
        if (!Double.isFinite(n) || n < min || n > max) throw new IllegalArgumentException("Invalid number");
        return n;
    }
    private static Position position(JsonObject o) {
        return new Position(number(o,"x",-30000000,30000000),number(o,"y",-10000,10000),number(o,"z",-30000000,30000000));
    }
    private static Stats stats(JsonObject o) {
        double max = number(o,"maxHealth",1,2048);
        return new Stats(number(o,"health",0,max),max,(int)number(o,"hunger",0,20),
                o.get("armorPercent").isJsonNull() ? null : number(o,"armorPercent",0,100),text(o,"mainItem"),text(o,"offItem"));
    }
    public static View snapshot(JsonObject o, String epoch, String team, long requestStart) {
        if (o.get("protocol").getAsInt()!=1 || !epoch.equals(o.get("epoch").getAsString())) throw new IllegalArgumentException("Protocol changed");
        JsonArray rows=o.getAsJsonArray("members"), points=o.getAsJsonArray("pings");
        if (rows.size()>10 || points.size()>10) throw new IllegalArgumentException("Team limit");
        List<Member> members=new ArrayList<>(); List<Ping> pings=new ArrayList<>();
        for (JsonElement e:rows) {
            JsonObject m=e.getAsJsonObject(); String role=text(m,"role");
            if (!ROLES.contains(role)) throw new IllegalArgumentException("Invalid role");
            long ttl=(long)number(m,"validForMs",0,5000);
            members.add(new Member(id(m,"id"),text(m,"name"),role,m.get("online").getAsBoolean(),
                    m.get("location").isJsonNull()?null:position(m.getAsJsonObject("location")),
                    m.get("stats").isJsonNull()?null:stats(m.getAsJsonObject("stats")),requestStart+ttl*1000000));
        }
        long time=o.get("serverTime").getAsLong();
        for(JsonElement e:points) {
            JsonObject p=e.getAsJsonObject(); String kind=text(p,"kind");
            if (!KINDS.contains(kind)) throw new IllegalArgumentException("Invalid ping");
            long ttl=Math.max(0,Math.min(30000,p.get("expiresAt").getAsLong()-time));
            pings.add(new Ping(text(p,"id"),text(p,"senderId"),kind,position(p.getAsJsonObject("position")),requestStart+ttl*1000000));
        }
        return new View(team,members,pings);
    }
    private LinkData() { }
}
