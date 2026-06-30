package db.xenova.bungee;

import db.xenova.DisBotCore;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

@SuppressWarnings("unused")
public final class DisBotBungee extends Plugin implements Listener {

    private DisBotCore core;

    public void onEnable() {
        long startTime = System.currentTimeMillis();
        core = new DisBotCore(getDataFolder(), getLogger(), new BungeeAdapter(getProxy(), startTime));

        core.start(reloadCallback ->
                getProxy().getPluginManager().registerCommand(this, new Command("disbot") {
                    @Override
                    public void execute(net.md_5.bungee.api.CommandSender sender, String[] args) {
                        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
                            sender.sendMessage(new TextComponent("Usage: /disbot reload"));
                            return;
                        }
                        reloadCallback.reload(() ->
                                sender.sendMessage(new TextComponent("Reload complete."))
                        );
                    }
                })
        );

        getProxy().getPluginManager().registerListener(this, this);
    }

    public void onDisable() {
        if (core != null) core.stop();
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (core == null) return;
        if (event.isCommand()) return;
        if (!(event.getSender() instanceof ProxiedPlayer player)) return;

        String playerName = player.getName();
        String message    = event.getMessage();
        String prefix     = core.getProxy().getPlayerPrefix(playerName);

        core.sendToDiscord(playerName, message, prefix);
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        if (core == null) return;
        core.sendJoinEmbed(event.getPlayer().getName());
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        if (core == null) return;
        core.sendLeaveEmbed(event.getPlayer().getName());
    }
}