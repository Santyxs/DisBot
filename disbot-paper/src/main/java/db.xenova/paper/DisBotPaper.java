package db.xenova.paper;

import db.xenova.DisBotCore;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class DisBotPaper extends JavaPlugin {

    private DisBotCore core;

    public void onEnable() {
        core = new DisBotCore(getDataFolder(), getLogger(), new PaperAdapter());
        core.start(reloadCallback -> {
            CommandExecutor executor = (CommandSender sender, Command cmd, String label, String[] args) -> {
                if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
                    sender.sendMessage(Component.text("Usage: disbot reload"));
                    return true;
                }
                reloadCallback.reload(() ->
                        sender.sendMessage(Component.text("Reload complete."))
                );
                return true;
            };
            Objects.requireNonNull(getCommand("disbot")).setExecutor(executor);
        });
    }

    public void onDisable() {
        if (core != null) core.stop();
    }
}