package db.xenova.discord;

import db.xenova.core.CustomCommandManager.CustomCommand;
import db.xenova.platform.ProxyAdapter;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class MessageResolver {

    private MessageResolver() {}

    public static EmbedBuilder buildEmbed(CustomCommand cmd, ProxyAdapter proxy) {
        EmbedBuilder embed = new EmbedBuilder()
                .setDescription(resolveVariables(cmd.message(), proxy))
                .setColor(parseColor(cmd.embedColor()));

        if (!cmd.footer().isBlank()) {
            embed.setFooter(resolveVariables(cmd.footer(), proxy));
        }

        if (!cmd.thumbnail().isBlank() && isValidUrl(cmd.thumbnail())) {
            embed.setThumbnail(cmd.thumbnail());
        }

        if (!cmd.image().isBlank() && isValidUrl(cmd.image())) {
            embed.setImage(cmd.image());
        }

        return embed;
    }

    public static String resolveVariables(String text, ProxyAdapter proxy) {
        List<String> players = proxy.getOnlinePlayerNames();

        String playerList = players.isEmpty() ? "No players online" : String.join(", ", players);
        String date       = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String time       = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String uptime     = formatUptime(System.currentTimeMillis() - proxy.getStartTime());

        return text
                .replace("{playerlist}", playerList)
                .replace("{players}", String.valueOf(players.size()))
                .replace("{status}", "🟢 Online")
                .replace("{date}", date)
                .replace("{time}", time)
                .replace("{uptime}", uptime);
    }

    private static String formatUptime(long millis) {
        long seconds = millis / 1000;
        long days    = seconds / 86400;
        long hours   = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs    = seconds % 60;

        if (days > 0)    return days + "d " + hours + "h " + minutes + "m";
        if (hours > 0)   return hours + "h " + minutes + "m " + secs + "s";
        if (minutes > 0) return minutes + "m " + secs + "s";
        return secs + "s";
    }

    private static boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    static Color parseColor(String hex) {
        try {
            return Color.decode(hex);
        } catch (NumberFormatException e) {
            return Color.decode("#57F287");
        }
    }
}