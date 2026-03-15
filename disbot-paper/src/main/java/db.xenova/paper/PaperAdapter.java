package db.xenova.paper;

import db.xenova.platform.ProxyAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public final class PaperAdapter implements ProxyAdapter {

    private final long startTime;

    public PaperAdapter(long startTime) {
        this.startTime = startTime;
    }

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

    public long getStartTime() {
        return startTime;
    }
}