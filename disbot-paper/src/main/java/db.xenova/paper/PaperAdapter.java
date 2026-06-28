package db.xenova.paper;

import db.xenova.platform.ProxyAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public record PaperAdapter(long startTime) implements ProxyAdapter {

    public void dispatchConsoleCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    public List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
    }

    public String getPlatformName() {
        return "Paper";
    }

    public void broadcastMessage(String message) {
        Component component = LegacyComponentSerializer.legacySection().deserialize(message);
        Bukkit.broadcast(component);
    }

    public String getPlayerPrefix(String playerName) {
        try {
            Player player = Bukkit.getPlayerExact(playerName);
            if (player == null) return "";

            User user = LuckPermsProvider.get()
                    .getUserManager()
                    .getUser(player.getUniqueId());
            if (user == null) return "";

            CachedMetaData meta = user.getCachedData().getMetaData();
            String prefix = meta.getPrefix();
            return prefix != null ? prefix : "";
        } catch (Exception e) {
            return "";
        }
    }
}