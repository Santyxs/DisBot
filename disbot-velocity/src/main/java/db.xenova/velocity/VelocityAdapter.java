package db.xenova.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import db.xenova.platform.ProxyAdapter;
import java.util.List;

public final class VelocityAdapter implements ProxyAdapter {

    private final ProxyServer proxy;

    public VelocityAdapter(ProxyServer proxy) {
        this.proxy = proxy;
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
}