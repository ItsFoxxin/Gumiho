package dev.gumiho.client;

/** Build-time capabilities: unsupported native features are never registered. */
public final class BuildInfo {
    public static final String VERSION = "0.9.5-link.3";
    public static final String MINECRAFT = "1.21.1";
    public static final int JAVA = 21;
    public static final boolean NATIVE_SPEARS = false;
    public static final String LABEL = "v" + VERSION + " / MC " + MINECRAFT;
    private BuildInfo() { }
}
