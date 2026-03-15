package db.xenova.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import db.xenova.platform.ProxyAdapter;

import java.util.List;

public final class VelocityAdapter implements ProxyAdapter {

    private final ProxyServer proxy;
    private final long startTime;

    public VelocityAdapter(ProxyServer proxy, long startTime) {
        this.proxy     = proxy;
        this.startTime = startTime;
    }

    public void dispatchConsoleCommand(String command) {
        proxy.getCommandManager().executeAsync(proxy.getConsoleCommandSource(), command);
    }

    public List<String> getOnlinePlayerNames() {
        return proxy.getAllPlayers().stream()
                .map(Player::getUsername)
                .toList();
    }

    public String getPlatformName() {
        return "Velocity";
    }

    public long getStartTime() {
        return startTime;
    }
}