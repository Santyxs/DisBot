package db.xenova.bungee;

import db.xenova.platform.ProxyAdapter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;

public class BungeeAdapter implements ProxyAdapter {

    private final ProxyServer proxy;
    private final long startTime;

    public BungeeAdapter(ProxyServer proxy, long startTime) {
        this.proxy     = proxy;
        this.startTime = startTime;
    }

    public void dispatchConsoleCommand(String command) {
        proxy.getPluginManager().dispatchCommand(proxy.getConsole(), command);
    }

    public List<String> getOnlinePlayerNames() {
        return proxy.getPlayers().stream()
                .map(ProxiedPlayer::getName)
                .toList();
    }

    public String getPlatformName() {
        return "BungeeCord";
    }

    public long startTime() {
        return startTime;
    }

    public void broadcastMessage(String message) {
        TextComponent component = new TextComponent(TextComponent.fromLegacy(message));
        proxy.broadcast(component);
    }

    public String getPlayerPrefix(String playerName) {
        return "";
    }
}