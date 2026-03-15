package db.xenova.discord;

import db.xenova.core.CustomCommandManager;
import db.xenova.core.CustomCommandManager.CustomCommand;
import db.xenova.platform.ProxyAdapter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.util.*;
import java.util.logging.Logger;

public final class SlashCommandsListener extends ListenerAdapter {

    private final String botName;
    private final List<String> allowedChannels;
    private final CustomCommandManager commandManager;
    private final ProxyAdapter proxy;
    private final Logger logger;

    public SlashCommandsListener(String botName,
                                 List<String> allowedChannels,
                                 CustomCommandManager commandManager,
                                 ProxyAdapter proxy,
                                 Logger logger) {
        this.botName         = botName;
        this.allowedChannels = allowedChannels;
        this.commandManager  = commandManager;
        this.proxy           = proxy;
        this.logger          = logger;
    }

    public List<SlashCommandData> buildSlashCommandData() {
        List<SlashCommandData> data = new ArrayList<>();
        data.add(Commands.slash("status", "Check if the bot is online"));

        Set<String> registered = new LinkedHashSet<>();
        registered.add("status");

        for (CustomCommand cmd : commandManager.getAll()) {
            String desc = cmd.description().length() > 100
                    ? cmd.description().substring(0, 97) + "..."
                    : cmd.description();

            if (registered.add(cmd.name())) {
                data.add(Commands.slash(cmd.name(), desc));
            }

            for (String alias : cmd.aliases()) {
                if (registered.add(alias.toLowerCase())) {
                    data.add(Commands.slash(alias.toLowerCase(), desc));
                }
            }
        }

        return data;
    }

    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String cmdName = event.getName().toLowerCase();

        if (!allowedChannels.isEmpty() && !allowedChannels.contains(event.getChannelId())) {
            event.reply("❌ This command is not allowed in this channel.").setEphemeral(true).queue();
            return;
        }

        if (cmdName.equals("status")) {
            event.reply("✅ " + botName + " is online.").queue();
            return;
        }

        Optional<CustomCommand> found = commandManager.find(cmdName);

        if (found.isPresent()) {
            CustomCommand cmd = found.get();

            long remaining = commandManager.checkAndApplyCooldown(event.getUser().getId(), cmd);
            if (remaining > 0) {
                event.reply("⏳ Please wait " + (remaining / 1000 + 1) + "s before using this command again.")
                        .setEphemeral(true).queue();
                return;
            }

            ReplyCallbackAction reply;
            if (cmd.embed()) {
                reply = event.replyEmbeds(MessageResolver.buildEmbed(cmd, proxy).build());
            } else {
                reply = event.reply(MessageResolver.resolveVariables(cmd.message(), proxy));
            }

            reply.setEphemeral(cmd.ephemeral()).queue();
            logger.fine("Slash /" + cmdName + " by " + event.getUser().getName());
        } else {
            event.reply("❌ Unknown command: `/" + cmdName + "`").setEphemeral(true).queue();
        }
    }
}