package db.xenova.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

    public long startTime() {
        return startTime;
    }

    public void broadcastMessage(String message) {
        Component component = LegacyComponentSerializer.legacySection().deserialize(message);
        proxy.getAllPlayers().forEach(p -> p.sendMessage(component));
    }

    public String getPlayerPrefix(String playerName) {
        return "";
    }
}