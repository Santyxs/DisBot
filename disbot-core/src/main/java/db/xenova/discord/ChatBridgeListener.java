package db.xenova.discord;

import db.xenova.platform.ProxyAdapter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import java.util.List;
import java.util.logging.Logger;

public final class ChatBridgeListener extends ListenerAdapter {

    private final List<String> bridgeChannels;
    private final ProxyAdapter proxy;
    private final Logger logger;
    private final String discordToMinecraftFormat;

    public ChatBridgeListener(List<String> bridgeChannels,
                              ProxyAdapter proxy,
                              Logger logger,
                              String discordToMinecraftFormat) {
        this.bridgeChannels           = bridgeChannels;
        this.proxy                    = proxy;
        this.logger                   = logger;
        this.discordToMinecraftFormat = discordToMinecraftFormat;
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (!bridgeChannels.isEmpty() && !bridgeChannels.contains(event.getChannel().getId())) return;

        String raw = event.getMessage().getContentRaw().trim();

        if (raw.startsWith("db!") || raw.startsWith("/")) return;

        if (raw.isBlank()) return;

        String username = event.getAuthor().getName();

        String role = "";
        if (event.isFromGuild() && event.getMember() != null) {
            role = event.getMember().getRoles().stream()
                    .findFirst()
                    .map(r -> "[" + r.getName() + "]")
                    .orElse("");
        }

        String formatted = discordToMinecraftFormat
                .replace("{username}", username)
                .replace("{message}", raw)
                .replace("{role}", role);

        formatted = translateColorCodes(formatted);

        proxy.broadcastMessage(formatted);
        logger.fine("[Discord → MC] " + username + " (" + role + "): " + raw);
    }

    private static String translateColorCodes(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(chars[i + 1]) != -1) {
                sb.append('§');
                sb.append(chars[i + 1]);
                i++;
            } else {
                sb.append(chars[i]);
            }
        }
        if (!text.isEmpty()) {
            char last = chars[chars.length - 1];
            if (chars.length < 2 || chars[chars.length - 2] != '&') {
                sb.append(last);
            }
        }
        return sb.toString();
    }
}