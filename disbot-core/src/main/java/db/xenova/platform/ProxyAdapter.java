package db.xenova.platform;

import java.util.List;

public interface ProxyAdapter {

    void dispatchConsoleCommand(String command);

    List<String> getOnlinePlayerNames();

    String getPlatformName();

    long startTime();

    void broadcastMessage(String message);

    String getPlayerPrefix(String playerName);
}