package db.xenova.discord;

import db.xenova.core.CustomCommandManager;
import db.xenova.core.CustomCommandManager.CustomCommand;
import db.xenova.platform.ProxyAdapter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.*;
import java.util.logging.Logger;

public final class PrefixCommandsListener extends ListenerAdapter {

    private final String prefix;
    private final String botName;
    private final List<String> allowedChannels;
    private final CustomCommandManager commandManager;
    private final ProxyAdapter proxy;
    private final Logger logger;

    public PrefixCommandsListener(String prefix,
                                  String botName,
                                  List<String> allowedChannels,
                                  CustomCommandManager commandManager,
                                  ProxyAdapter proxy,
                                  Logger logger) {
        this.prefix          = prefix.toLowerCase();
        this.botName         = botName;
        this.allowedChannels = allowedChannels;
        this.commandManager  = commandManager;
        this.proxy           = proxy;
        this.logger          = logger;
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (!allowedChannels.isEmpty() && !allowedChannels.contains(event.getChannel().getId())) return;

        String raw = event.getMessage().getContentRaw().trim();
        if (!raw.toLowerCase().startsWith(prefix)) return;

        String argument = raw.substring(prefix.length()).trim().toLowerCase();

        if (argument.isEmpty() || argument.equals("status")) {
            event.getMessage().reply("✅ " + botName + " is online.").queue();
            return;
        }

        Optional<CustomCommand> found = commandManager.find(argument);

        if (found.isPresent()) {
            CustomCommand cmd = found.get();

            long remaining = commandManager.checkAndApplyCooldown(event.getAuthor().getId(), cmd);
            if (remaining > 0) {
                event.getMessage()
                        .reply("⏳ Please wait " + (remaining / 1000 + 1) + "s before using this command again.")
                        .queue();
                return;
            }

            if (cmd.embed()) {
                event.getMessage().replyEmbeds(MessageResolver.buildEmbed(cmd, proxy).build()).queue();
            } else {
                event.getMessage().reply(MessageResolver.resolveVariables(cmd.message(), proxy)).queue();
            }
            logger.fine("Command executed: " + argument + " by " + event.getAuthor().getName());
        } else {
            event.getMessage()
                    .reply("❌ Unknown command: `" + argument + "`.")
                    .queue();
        }
    }
}