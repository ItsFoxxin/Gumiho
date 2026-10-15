package net.minecraft.client;
import net.minecraft.client.multiplayer.ServerData;
public final class Minecraft {
    public Object options, font, player, level, screen;
    public final Gui gui=new Gui();
    public final class Gui { public Object screen() { return screen; } }
    public boolean singleplayer;
    public ServerData server;
    public static Minecraft getInstance() { throw new AssertionError("Mod construction touched the singleton"); }
    public boolean hasSingleplayerServer() { return singleplayer; }
    public ServerData getCurrentServer() { return server; }
    public Window getWindow() { return new Window(); }
    public static final class Window {
        public int getGuiScaledWidth() { return 480; }
        public int getGuiScaledHeight() { return 270; }
    }
}
