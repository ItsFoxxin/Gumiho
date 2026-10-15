package dev.gumiho.client.module.utility;

/** Presentation only. Latency comes from the server's player list, not a new probe. */
public final class NetworkStatus {
    public static String latency(boolean connected, boolean local, Integer milliseconds) {
        if (!connected) return "Offline | 연결 없음";
        if (local) return "Local | 로컬";
        if (milliseconds == null || milliseconds < 0) return "Ping -- | 알 수 없음";
        return "Ping " + milliseconds + " ms";
    }
    public static boolean highLatency(boolean connected, boolean local, Integer milliseconds) {
        return connected && !local && milliseconds != null && milliseconds > 150;
    }
    private NetworkStatus() { }
}
