package db.xenova.paper;

import db.xenova.DisBotCore;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class DisBotPaper extends JavaPlugin implements Listener {

    private DisBotCore core;

    public void onEnable() {
        long startTime = System.currentTimeMillis();
        core = new DisBotCore(getDataFolder(), getLogger(), new PaperAdapter(startTime));

        core.start(reloadCallback -> {
            CommandExecutor executor = (CommandSender sender, Command cmd, String label, String[] args) -> {
                if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
                    sender.sendMessage(Component.text("Usage: /disbot reload"));
                    return true;
                }
                reloadCallback.reload(() ->
                        sender.sendMessage(Component.text("Reload complete."))
                );
                return true;
            };
            Objects.requireNonNull(getCommand("disbot")).setExecutor(executor);
        });

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if (core != null) core.stop();
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (core == null) return;

        String playerName = event.getPlayer().getName();
        String message    = PlainTextComponentSerializer.plainText().serialize(event.message());
        String prefix     = core.getProxy().getPlayerPrefix(playerName);

        core.sendToDiscord(playerName, message, prefix);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (core == null) return;
        core.sendJoinEmbed(event.getPlayer().getName());
    }
}