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
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerPong;
import de.myzelyam.supervanish.SuperVanish;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.logging.Level;

public class ServerListPacketListener implements PacketListener {

    private final SuperVanish plugin;

    private boolean errorLogged = false;

    public ServerListPacketListener(SuperVanish plugin) {
        this.plugin = plugin;
    }

    public static void register(SuperVanish plugin) {
        plugin.getLogger().log(Level.INFO, "Hooked into PaperSpigot for server list ping support");
        plugin.getServer().getPluginManager().registerEvents(new PaperServerPingListener(plugin), plugin);
    }

    public static boolean isEnabled(SuperVanish plugin) {
        final FileConfiguration config = plugin.getSettings();
        return config.getBoolean(
                "ExternalInvisibility.ServerList.AdjustAmountOfOnlinePlayers")
                || config.getBoolean(
                "ExternalInvisibility.ServerList.AdjustListOfLoggedInPlayers");
    }
}