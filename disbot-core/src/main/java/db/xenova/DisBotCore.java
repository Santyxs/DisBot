package db.xenova;

import db.xenova.core.CustomCommandLoader;
import db.xenova.core.CustomCommandManager;
import db.xenova.discord.ChatBridgeListener;
import db.xenova.discord.PrefixCommandsListener;
import db.xenova.discord.SlashCommandsListener;
import db.xenova.platform.ProxyAdapter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DisBotCore {

    private final File dataFolder;
    private final Logger logger;
    private final ProxyAdapter proxy;

    private JDA jda;
    private CustomCommandLoader loader;
    private SlashCommandsListener slashListener;

    private List<String> bridgeChannels = List.of();
    private String mcToDiscordFormat    = "**{luckperms_prefix} {username}:** {message}";

    private boolean joinEmbedEnabled    = false;
    private String joinEmbedMessage     = "**{username}** se ha unido al servidor.";
    private String joinEmbedColor       = "#57F287";
    private String joinEmbedThumbnail   = "";

    private boolean leaveEmbedEnabled   = false;
    private String leaveEmbedMessage    = "**{username}** ha salido del servidor.";
    private String leaveEmbedColor      = "#ED4245";
    private String leaveEmbedThumbnail  = "";

    public DisBotCore(File dataFolder, Logger logger, ProxyAdapter proxy) {
        this.dataFolder = dataFolder;
        this.logger     = logger;
        this.proxy      = proxy;
    }

    // ─── Getters ─────────────────────────────────────────────────────

    public ProxyAdapter getProxy() {
        return proxy;
    }

    // ─── Bridge MC → Discord ──────────────────────────────────────────────────

    public void sendToDiscord(String playerName, String message, String luckpermsPrefix) {
        if (jda == null) return;
        if (bridgeChannels.isEmpty()) return;

        String formatted = mcToDiscordFormat
                .replace("{username}", playerName)
                .replace("{message}", message)
                .replace("{luckperms_prefix}", luckpermsPrefix);

        formatted = formatted.replaceAll("§[0-9a-fk-orA-FK-OR]", "");

        final String finalFormatted = formatted;
        for (String channelId : bridgeChannels) {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(finalFormatted).queue(
                        ok  -> logger.fine("[MC→Discord] " + finalFormatted),
                        err -> logger.warning("[MC→Discord] Error en canal " + channelId + ": " + err.getMessage())
                );
            } else {
                logger.warning("[MC→Discord] Canal no encontrado: " + channelId);
            }
        }
    }

    // ─── Join / Leave embeds MC → Discord ─────────────────────────────────────

    public void sendJoinEmbed(String playerName) {
        sendStatusEmbed(joinEmbedEnabled, joinEmbedMessage, joinEmbedColor, joinEmbedThumbnail, playerName, "Join");
    }

    public void sendLeaveEmbed(String playerName) {
        sendStatusEmbed(leaveEmbedEnabled, leaveEmbedMessage, leaveEmbedColor, leaveEmbedThumbnail, playerName, "Leave");
    }

    private void sendStatusEmbed(boolean enabled, String message, String hexColor, String thumbnail, String playerName, String logLabel) {
        if (jda == null) return;
        if (!enabled) return;
        if (bridgeChannels.isEmpty()) return;

        String description = message.replace("{username}", playerName);

        Color color;
        try {
            color = Color.decode(hexColor);
        } catch (NumberFormatException e) {
            color = Color.decode("#57F287");
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setDescription(description)
                .setColor(color);

        if (!thumbnail.isBlank()) {
            embed.setThumbnail(thumbnail);
        }

        for (String channelId : bridgeChannels) {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessageEmbeds(embed.build()).queue(
                        ok  -> logger.fine("[MC→Discord] " + logLabel + " embed: " + playerName),
                        err -> logger.warning("[MC→Discord] Error enviando " + logLabel + " embed: " + err.getMessage())
                );
            } else {
                logger.warning("[MC→Discord] Canal no encontrado: " + channelId);
            }
        }
    }

    // ─── Life Cycle ────────────────────────────────────────────────────────

    public void start(Consumer<ReloadCallback> registerCommand) {
        logger.info("╔══════════════════════════╗");
        logger.info("║  DisBot - Starting ...   ║");
        logger.info("╚══════════════════════════╝");

        registerCommand.accept(this::reload);

        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.severe("Could not create data folder: " + dataFolder.getAbsolutePath());
            return;
        }

        // ── Config ────────────────────────────────────────────────────────────
        File configFile = new File(dataFolder, "config.yml");
        copyDefaultIfMissing(configFile, "config.yml");

        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        Map<String, Object> config;
        try (var in = new java.io.FileInputStream(configFile)) {
            config = yaml.load(in);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading config.yml", e);
            return;
        }

        String token                 = getString(config, "discord-token", "");
        String prefix                = getString(config, "command-prefix", "db!");
        String botName               = getString(config, "bot-name", "DisBot");
        List<String> allowedChannels = getStringListByKey(config, "allowed-channels");
        bridgeChannels               = getStringListByKey(config, "server-channel-id");
        String discordToMcFormat     = getString(config, "discord-to-minecraft-format", "&9[&bDC]&9 {role}  &c{username} &7: &f{message}");
        mcToDiscordFormat            = getString(config, "minecraft-to-discord-format", "**{luckperms_prefix} {username}:** {message}");
        joinEmbedEnabled             = getBoolean(config, "join-embed-enabled");
        joinEmbedMessage             = getString(config, "join-embed-message", "**{username}** se ha unido al servidor.");
        joinEmbedColor               = getString(config, "join-embed-color", "#57F287");
        joinEmbedThumbnail           = getString(config, "join-embed-thumbnail", "");
        leaveEmbedEnabled            = getBoolean(config, "leave-embed-enabled");
        leaveEmbedMessage            = getString(config, "leave-embed-message", "**{username}** ha salido del servidor.");
        leaveEmbedColor              = getString(config, "leave-embed-color", "#ED4245");
        leaveEmbedThumbnail          = getString(config, "leave-embed-thumbnail", "");

        if (token.isBlank() || token.equals("TOKEN-HERE")) {
            logger.severe("Please set 'discord-token' in config.yml.");
            return;
        }

        if (allowedChannels.isEmpty()) {
            logger.info("No channel restriction set — responding in all channels.");
        } else {
            logger.info("Allowed channels: " + allowedChannels);
        }

        if (bridgeChannels.isEmpty()) {
            logger.warning("No bridge channels set (server-channel-id). Chat bridge MC→Discord disabled.");
        } else {
            logger.info("Bridge channels: " + bridgeChannels);
        }

        logger.info("Platform: " + proxy.getPlatformName());

        // ── Custom Commands ───────────────────────────────────────────
        CustomCommandManager commandManager = new CustomCommandManager();
        File commandsFolder = new File(dataFolder, "commands");
        copyDefaultCommandsIfMissing(commandsFolder);
        loader = new CustomCommandLoader(commandsFolder, commandManager, logger);
        loader.loadAll();

        // ── Discord Listeners ──────────────────────────────────────────────
        PrefixCommandsListener prefixListener = new PrefixCommandsListener(
                prefix, botName, allowedChannels, commandManager, proxy, logger
        );
        slashListener = new SlashCommandsListener(
                botName, allowedChannels, commandManager, proxy, logger
        );

        List<String> chatListenChannels = bridgeChannels.isEmpty() ? allowedChannels : bridgeChannels;
        ChatBridgeListener chatBridgeListener = new ChatBridgeListener(
                chatListenChannels, proxy, logger, discordToMcFormat
        );

        // ── Connection JDA ──────────────────────────────────────────────────────
        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(prefixListener, slashListener, chatBridgeListener)
                    .build()
                    .awaitReady();

            logger.info("Connected to Discord as: " + jda.getSelfUser().getAsTag());
            registerSlashCommands();
            logger.info("✔ DisBot active. Prefix: " + prefix);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, "Discord connection interrupted.", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to Discord.", e);
        }
    }

    public void stop() {
        if (jda != null) {
            logger.info("Closing Discord connection...");
            jda.shutdown();
        }
        logger.info("Plugin stopped.");
    }

    // ─── Reload ───────────────────────────────────────────────────────────────

    @FunctionalInterface
    public interface ReloadCallback {
        void reload(Runnable onDone);
    }

    public void reload(Runnable onDone) {
        if (loader == null || jda == null) {
            logger.warning("Cannot reload: plugin is not fully started.");
            return;
        }
        loader.loadAll();
        jda.updateCommands()
                .addCommands(slashListener.buildSlashCommandData())
                .queue(cmds -> {
                    logger.info("Slash commands registered: " + cmds.size());
                    onDone.run();
                });
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private void registerSlashCommands() {
        jda.updateCommands()
                .addCommands(slashListener.buildSlashCommandData())
                .queue(cmds -> logger.info("Slash commands registered: " + cmds.size()));
    }

    private void copyDefaultIfMissing(File target, String resourceName) {
        if (target.exists()) return;
        try (InputStream in = DisBotCore.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                logger.warning("Resource not found in jar: " + resourceName);
                return;
            }
            Files.copy(in, target.toPath());
            logger.info("Created: " + target.getName());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not copy " + resourceName, e);
        }
    }

    private void copyDefaultCommandsIfMissing(File commandsFolder) {
        if (!commandsFolder.exists()) {
            if (!commandsFolder.mkdirs()) {
                logger.warning("Could not create commands folder.");
                return;
            }
            copyDefaultIfMissing(new File(commandsFolder, "example.yml"), "commands/example.yml");
            copyDefaultIfMissing(new File(commandsFolder, "playerslist.yml"), "commands/playerslist.yml");
            copyDefaultIfMissing(new File(commandsFolder, "help.yml"), "commands/help.yml");
        }
    }

    private static String getString(Map<String, Object> map, String key, String fallback) {
        Object val = map.get(key);
        return (val instanceof String s) ? s : fallback;
    }

    private static boolean getBoolean(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val instanceof Boolean b) ? b : false;
    }

    private static List<String> getStringListByKey(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) {
            return list.stream()
                    .filter(e -> e instanceof String)
                    .map(e -> (String) e)
                    .toList();
        }
        return List.of();
    }
}