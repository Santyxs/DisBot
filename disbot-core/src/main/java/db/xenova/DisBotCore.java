package db.xenova;

import db.xenova.core.CustomCommandLoader;
import db.xenova.core.CustomCommandManager;
import db.xenova.discord.PrefixCommandsListener;
import db.xenova.discord.SlashCommandsListener;
import db.xenova.platform.ProxyAdapter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

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

    public DisBotCore(File dataFolder, Logger logger, ProxyAdapter proxy) {
        this.dataFolder = dataFolder;
        this.logger     = logger;
        this.proxy      = proxy;
    }

    public void start(Consumer<ReloadCallback> registerCommand) {
        logger.info("╔══════════════════════════╗");
        logger.info("║  DisBot - Starting ...   ║");
        logger.info("╚══════════════════════════╝");

        registerCommand.accept(this::reload);

        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.severe("Could not create data folder: " + dataFolder.getAbsolutePath());
            return;
        }

        File configFile = new File(dataFolder, "config.yml");
        copyDefaultIfMissing(configFile, "config.yml");

        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        java.util.Map<String, Object> config;
        try (var in = new java.io.FileInputStream(configFile)) {
            config = yaml.load(in);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading config.yml", e);
            return;
        }

        String token                 = getString(config, "discord-token", "");
        String prefix                = getString(config, "command-prefix", "db!");
        String botName               = getString(config, "bot-name", "DisBot");
        List<String> allowedChannels = getStringList(config);

        if (token.isBlank() || token.equals("TOKEN-HERE")) {
            logger.severe("Please set 'discord-token' in config.yml.");
            return;
        }

        if (allowedChannels.isEmpty()) {
            logger.info("No channel restriction set — responding in all channels.");
        } else {
            logger.info("Allowed channels: " + allowedChannels);
        }

        logger.info("Platform: " + proxy.getPlatformName());

        CustomCommandManager commandManager = new CustomCommandManager();
        File commandsFolder = new File(dataFolder, "commands");
        copyDefaultCommandsIfMissing(commandsFolder);
        loader = new CustomCommandLoader(commandsFolder, commandManager, logger);
        loader.loadAll();

        PrefixCommandsListener prefixListener = new PrefixCommandsListener(
                prefix, botName, allowedChannels, commandManager, proxy, logger
        );
        slashListener = new SlashCommandsListener(
                botName, allowedChannels, commandManager, proxy, logger
        );

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(prefixListener, slashListener)
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
            copyDefaultIfMissing(new File(commandsFolder, "help.yml"), "commands/help.yml");
        }
    }

    private static String getString(Map<String, Object> map, String key, String fallback) {
        Object val = map.get(key);
        return (val instanceof String s) ? s : fallback;
    }

    private static List<String> getStringList(Map<String, Object> map) {
        Object val = map.get("allowed-channels");
        if (val instanceof List<?> list) {
            return list.stream()
                    .filter(e -> e instanceof String)
                    .map(e -> (String) e)
                    .toList();
        }
        return List.of();
    }
}