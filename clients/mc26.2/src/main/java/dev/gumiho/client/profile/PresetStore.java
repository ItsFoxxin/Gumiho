package dev.gumiho.client.profile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Named JSON files; saves never replace an existing setup. */
public final class PresetStore {
    private final Path directory;
    public PresetStore(Path directory) { this.directory = directory.toAbsolutePath().normalize(); }
    public Path directory() { return directory; }
    public static String validName(String name) {
        if (name == null || !name.matches("[\\p{L}\\p{N}][\\p{L}\\p{N} _-]{0,39}") || !name.equals(name.strip())
                || Pattern.matches("(?i)(CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])", name))
            throw new IllegalArgumentException("Use 1-40 letters/numbers, spaces, - or _");
        return name;
    }
    private Path path(String name, PresetCodec.Kind kind) {
        return directory.resolve(kind.name().toLowerCase(Locale.ROOT)).resolve(validName(name) + ".json");
    }
    public void save(String name, PresetCodec.Kind kind, String json) throws IOException {
        Path file = path(name, kind);
        if (json.getBytes(StandardCharsets.UTF_8).length > 65536) throw new IOException("Preset is too large");
        Files.createDirectories(file.getParent());
        Files.writeString(file, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }
    public String read(String name, PresetCodec.Kind kind) throws IOException {
        Path file = path(name, kind);
        if (Files.size(file) > 65536) throw new IOException("Preset is too large");
        return Files.readString(file, StandardCharsets.UTF_8);
    }
    public List<String> names(PresetCodec.Kind kind) throws IOException {
        Path folder = directory.resolve(kind.name().toLowerCase(Locale.ROOT));
        if (!Files.isDirectory(folder)) return List.of();
        try (var files = Files.list(folder)) {
            return files.filter(Files::isRegularFile).map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".json")).map(n -> n.substring(0, n.length() - 5))
                    .filter(n -> { try { validName(n); return true; } catch (IllegalArgumentException e) { return false; } })
                    .sorted(String.CASE_INSENSITIVE_ORDER).limit(512).toList();
        }
    }
}
