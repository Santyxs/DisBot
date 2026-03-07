package db.xenova.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomCommandManager {

    public record CustomCommand(
            String name,
            String description,
            String message,
            String embedColor,
            boolean embed,
            boolean ephemeral,
            int cooldown,
            List<String> aliases
    ) {}

    private final Map<String, CustomCommand> commands = new LinkedHashMap<>();

    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public void register(CustomCommand command) {
        commands.put(command.name().toLowerCase(), command);
        for (String alias : command.aliases()) {
            commands.put(alias.toLowerCase(), command);
        }
    }

    public Optional<CustomCommand> find(String name) {
        return Optional.ofNullable(commands.get(name.toLowerCase()));
    }

    public Collection<CustomCommand> getAll() {
        return commands.values().stream().distinct().toList();
    }

    public void clear() {
        commands.clear();
        cooldowns.clear();
    }

    public int size() {
        return (int) commands.values().stream().distinct().count();
    }

    public long checkAndApplyCooldown(String userId, CustomCommand cmd) {
        if (cmd.cooldown() <= 0) return 0;

        String key = userId + ":" + cmd.name();
        long now = System.currentTimeMillis();

        evictExpired();

        long expiresAt = cooldowns.getOrDefault(key, 0L);
        long remaining = expiresAt - now;

        if (remaining > 0) return remaining;

        cooldowns.put(key, now + cmd.cooldown() * 1000L);
        return 0;
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> now > entry.getValue());
    }
}