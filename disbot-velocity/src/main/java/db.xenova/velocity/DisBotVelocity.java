package db.xenova.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import db.xenova.DisBotCore;
import net.kyori.adventure.text.Component;

import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(
        id = "disbot",
        name = "DisBot",
        version = "0.1",
        authors = {"xenova"}
)
@SuppressWarnings("unused")
public final class DisBotVelocity {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private DisBotCore core;

    @Inject
    public DisBotVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy         = proxy;
        this.logger        = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        long startTime = System.currentTimeMillis();
        core = new DisBotCore(dataDirectory.toFile(), logger, new VelocityAdapter(proxy, startTime));

        core.start(reloadCallback ->
                proxy.getCommandManager().register(
                        proxy.getCommandManager().metaBuilder("disbot").build(),
                        (SimpleCommand) invocation -> {
                            CommandSource sender = invocation.source();
                            String[] args = invocation.arguments();
                            if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
                                sender.sendMessage(Component.text("Usage: /disbot reload"));
                                return;
                            }
                            reloadCallback.reload(() ->
                                    sender.sendMessage(Component.text("Reload complete."))
                            );
                        }
                )
        );
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (core != null) core.stop();
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        if (core == null) return;

        String playerName = event.getPlayer().getUsername();
        String message    = event.getMessage();
        String prefix     = core.getProxy().getPlayerPrefix(playerName);

        core.sendToDiscord(playerName, message, prefix);
    }

    @Subscribe
    public void onJoin(PostLoginEvent event) {
        if (core == null) return;
        core.sendJoinEmbed(event.getPlayer().getUsername());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (core == null) return;
        core.sendLeaveEmbed(event.getPlayer().getUsername());
    }
}