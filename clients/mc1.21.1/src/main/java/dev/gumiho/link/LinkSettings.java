package dev.gumiho.link;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

/** Private, separate from all kit/HUD exports. Never saves snapshots or invitations. */
public final class LinkSettings {
    public String endpoint="http://127.0.0.1:8791";
    public String memberToken="";
    public LinkData.Sharing sharing=LinkData.Sharing.none();
    public static LinkSettings load(Path path) throws IOException {
        if(!Files.exists(path)) return new LinkSettings();
        if(Files.size(path)>8192) throw new IOException("Private settings too large");
        try {
            LinkSettings s=LinkData.JSON.fromJson(Files.readString(path),LinkSettings.class);
            s.validate(); return s;
        } catch(RuntimeException e) { throw new IOException("Invalid private settings; file left unchanged"); }
    }
    public void validate() {
        endpoint=LinkData.endpoint(endpoint).toString();
        if(!memberToken.isEmpty()) LinkData.token(memberToken,"gl1_");
        sharing=new LinkData.Sharing(sharing.locationRoles(),sharing.statsRoles());
    }
    public void save(Path path) throws IOException {
        validate(); Files.createDirectories(path.toAbsolutePath().getParent());
        Path temp=Files.createTempFile(path.toAbsolutePath().getParent(),".gumiho-link-",".tmp");
        try {
            Files.writeString(temp,LinkData.JSON.toJson(this),StandardCharsets.UTF_8);
            // Refuse non-atomic fallback: do not truncate an existing credential file.
            Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
    @Override public String toString() { return "LinkSettings[credentials redacted]"; }
}
