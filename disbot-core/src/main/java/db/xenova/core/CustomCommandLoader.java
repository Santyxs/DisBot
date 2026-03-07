package db.xenova.core;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CustomCommandLoader {

    private static final String DEFAULT_COLOR = "#57F287";

    private final File commandsFolder;
    private final CustomCommandManager manager;
    private final Logger logger;

    public CustomCommandLoader(File commandsFolder,
                               CustomCommandManager manager,
                               Logger logger) {
        this.commandsFolder = commandsFolder;
        this.manager        = manager;
        this.logger         = logger;
    }

    public void loadAll() {
        if (!commandsFolder.exists()) {
            if (!commandsFolder.mkdirs()) {
                logger.warning("Could not create commands folder.");
                return;
            }
            logger.info("Commands folder created.");
        }

        manager.clear();

        File[] files = commandsFolder.listFiles(
                (dir, name) -> name.toLowerCase().endsWith(".yml")
        );

        if (files == null || files.length == 0) {
            logger.info("No commands found in commands folder.");
            return;
        }

        Yaml yaml = new Yaml();

        for (File file : files) {
            try (FileInputStream fis = new FileInputStream(file)) {

                Map<String, Object> data = yaml.load(fis);

                if (data == null) {
                    logger.warning("Empty file ignored: " + file.getName());
                    continue;
                }

                String cmdName    = getString(data, "name", "");
                String desc       = getString(data, "description", "No description");
                String message    = getString(data, "message", "");
                String embedColor = getString(data, "embed-color", DEFAULT_COLOR);
                boolean embed     = getBoolean(data, "embed");
                boolean ephemeral = getBoolean(data, "ephemeral");
                int cooldown      = getInt(data);
                List<String> aliases = getStringList(data);

                if (cmdName.isBlank()) {
                    logger.warning("Command without 'name' ignored: " + file.getName());
                    continue;
                }

                if (message.isBlank()) {
                    logger.warning("Command without 'message' ignored: " + file.getName());
                    continue;
                }

                manager.register(new CustomCommandManager.CustomCommand(
                        cmdName, desc, message, embedColor, embed, ephemeral, cooldown, aliases
                ));
                logger.info("Command loaded: " + cmdName
                        + (aliases.isEmpty() ? "" : " (aliases: " + aliases + ")"));

            } catch (IOException e) {
                logger.log(Level.WARNING, "Error reading " + file.getName(), e);
            }
        }

        logger.info("Total commands loaded: " + manager.size());
    }

    private static String getString(Map<String, Object> map, String key, String fallback) {
        Object val = map.get(key);
        return (val instanceof String s) ? s : fallback;
    }

    private static boolean getBoolean(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val instanceof Boolean b) ? b : false;
    }

    private static int getInt(Map<String, Object> map) {
        Object val = map.get("cooldown");
        return (val instanceof Integer i) ? i : 0;
    }

    private static List<String> getStringList(Map<String, Object> map) {
        Object val = map.get("aliases");
        if (val instanceof List<?> list) {
            return list.stream()
                    .filter(e -> e instanceof String)
                    .map(e -> (String) e)
                    .toList();
        }
        return List.of();
    }
}