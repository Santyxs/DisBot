package db.xenova.bungee;

import db.xenova.DisBotCore;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

public final class DisBotBungee extends Plugin {

    private DisBotCore core;

    public void onEnable() {
        core = new DisBotCore(getDataFolder(), getLogger(), new BungeeAdapter(getProxy()));
        core.start(reloadCallback ->
                getProxy().getPluginManager().registerCommand(this, new Command("disbot") {
                    public void execute(net.md_5.bungee.api.CommandSender sender, String[] args) {
                        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
                            sender.sendMessage(new TextComponent("Usage: disbot reload"));
                            return;
                        }
                        reloadCallback.reload(() ->
                                sender.sendMessage(new TextComponent("Reload complete."))
                        );
                    }
                })
        );
    }

    public void onDisable() {
        if (core != null) core.stop();
    }
}