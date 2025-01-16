/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.visibility;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerListHeaderAndFooter;
import com.github.retrooper.packetevents.wrapper.status.client.WrapperStatusClientPing;
import de.myzelyam.supervanish.SuperVanish;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.logging.Level;

public class ServerListPacketListener implements PacketListener {

    private final SuperVanish plugin;

    private boolean errorLogged = false;

    public ServerListPacketListener(SuperVanish plugin) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Status.Server.SERVER_INFO);
        this.plugin = plugin;
    }

    public static void register(SuperVanish plugin) {
        // Use Paper event listener if available
        try {
            Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
            plugin.getLogger().log(Level.INFO, "Hooked into PaperSpigot for server list ping support");
            plugin.getServer().getPluginManager().registerEvents(new PaperServerPingListener(plugin), plugin);
        } catch (ClassNotFoundException ignored) {
            PacketEvents.getAPI().getEventManager().registerListener(new ServerListPacketListener(plugin), PacketListenerPriority.NORMAL);
        }
    }

    public static boolean isEnabled(SuperVanish plugin) {
        final FileConfiguration config = plugin.getSettings();
        return config.getBoolean(
                "ExternalInvisibility.ServerList.AdjustAmountOfOnlinePlayers")
                || config.getBoolean(
                "ExternalInvisibility.ServerList.AdjustListOfLoggedInPlayers");
    }

    @Override
    public void onPacketSend(PacketSendEvent e) {
        try {
            final FileConfiguration settings = plugin.getSettings();
            if (!settings.getBoolean("ExternalInvisibility.ServerList.AdjustAmountOfOnlinePlayers")
                    && !settings.getBoolean("ExternalInvisibility.ServerList.AdjustListOfLoggedInPlayers"))
                return;

            WrapperPlayServerPlayerListHeaderAndFooter ping = new WrapperPlayServerPlayerListHeaderAndFooter(e);
            WrapperPlayServerPing ping = new WrapperPlayServerPing(e);
            WrappedServerPing ping = e.getPacket().getServerPings().read(0);
            Collection<UUID> onlineVanishedPlayers = plugin.getVanishStateMgr().getOnlineVanishedPlayers();
            int vanishedPlayersCount = plugin.getVanishStateMgr().getOnlineVanishedPlayers().size(),
                    playerCount = Bukkit.getOnlinePlayers().size();
            if (settings.getBoolean("ExternalInvisibility.ServerList.AdjustAmountOfOnlinePlayers")) {
                ping.setPlayersOnline(playerCount - vanishedPlayersCount);
            }
            if (settings.getBoolean("ExternalInvisibility.ServerList.AdjustListOfLoggedInPlayers")) {
                List<WrappedGameProfile> wrappedGameProfiles = new ArrayList<>(ping.getPlayers());
                Iterator<WrappedGameProfile> iterator = wrappedGameProfiles.iterator();
                while (iterator.hasNext()) {
                    if (onlineVanishedPlayers.contains(iterator.next().getUUID())) {
                        iterator.remove();
                    }
                }
                ping.setPlayers(wrappedGameProfiles);
            }
            e.getPacket().getServerPings().write(0, ping);
        } catch (Exception er) {
            if (!errorLogged) {
                if (er.getMessage() != null && er.getMessage().contains("Unable to construct new instance using public net.minecraft.network.protocol.status.ServerPing")) {
                    plugin.getLogger().warning("The spigot-sided serverlist features are not supported by packetevents on your server. Please make sure you are using the latest packetevents dev build. (" + er.getMessage() + ")\n");
                } else if (er.getMessage() != null && er.getMessage().contains("Cannot assign field \"online\" because \"this.playerSample\" is null")) {
                    plugin.getLogger().warning("The spigot-sided serverlist features are not supported yet by packetevents. Please make sure you are using the latest packetevents dev build.");
                } else {
                    plugin.logException(er);
                }
                errorLogged = true;
            }
        }
    }
}