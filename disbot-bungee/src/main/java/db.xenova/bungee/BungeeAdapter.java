package db.xenova.bungee;

import db.xenova.platform.ProxyAdapter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;

public final class BungeeAdapter implements ProxyAdapter {

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

    public long getStartTime() {
        return startTime;
    }
}