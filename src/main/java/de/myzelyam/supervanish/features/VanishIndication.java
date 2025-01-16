/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.features;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.google.common.collect.ImmutableList;
import de.myzelyam.api.vanish.PlayerShowEvent;
import de.myzelyam.api.vanish.PostPlayerHideEvent;
import de.myzelyam.supervanish.SuperVanish;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This is currently unused on Minecraft 1.19 or higher
 */
public class VanishIndication extends Feature {
    private boolean suppressErrors = false;

    public VanishIndication(SuperVanish plugin) {
        super(plugin);
    }

    @Override
    public boolean isActive() {
        return !plugin.getVersionUtil().isOneDotXOrHigher(19)
                && plugin.getSettings().getBoolean("IndicationFeatures.MarkVanishedPlayersAsSpectators");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVanish(PostPlayerHideEvent e) {
        Player p = e.getPlayer();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!plugin.getVisibilityChanger().getHider().isHidden(p, onlinePlayer) && p != onlinePlayer) {
                sendPlayerInfoChangeGameModePacket(onlinePlayer, p, true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onReappear(PlayerShowEvent e) {
        final Player p = e.getPlayer();
        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!plugin.getVisibilityChanger().getHider().isHidden(p, onlinePlayer) && p != onlinePlayer) {
                delay(() -> sendPlayerInfoChangeGameModePacket(onlinePlayer, p, false));
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        final Player p = e.getPlayer();
        delay(() -> {
            // tell others that p is a spectator
            if (plugin.getVanishStateMgr().isVanished(p.getUniqueId()))
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (!plugin.getVisibilityChanger().getHider().isHidden(p, onlinePlayer)
                            && p != onlinePlayer) {
                        sendPlayerInfoChangeGameModePacket(onlinePlayer, p, true);
                    }
                }
            // tell p that others are spectators
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!plugin.getVanishStateMgr().isVanished(onlinePlayer.getUniqueId())) continue;
                if (!plugin.getVisibilityChanger().getHider().isHidden(onlinePlayer, p)
                        && p != onlinePlayer) {
                    sendPlayerInfoChangeGameModePacket(p, onlinePlayer, true);
                }
            }
        });
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().getEventManager().registerListener(
                new PacketListener() {
                    @Override
                    public void onPacketSend(PacketSendEvent e) {
                        if (e.getPacketType() != PacketType.Play.Server.PLAYER_INFO) return;

                        try {
                            WrapperPlayServerPlayerInfo packet = new WrapperPlayServerPlayerInfo(e);
                            if (packet.getAction() != WrapperPlayServerPlayerInfo.Action.UPDATE_GAME_MODE) return;

                            Player receiver = e.getPlayer();

                            List<WrapperPlayServerPlayerInfo.PlayerData> infoDataList = new ArrayList<>(packet.getPlayerDataList());
                            for (WrapperPlayServerPlayerInfo.PlayerData infoData : ImmutableList.copyOf(infoDataList)) {
                                if (!VanishIndication.this.plugin.getVisibilityChanger().getHider()
                                        .isHidden(infoData.getUserProfile().getUUID(), receiver)
                                        && VanishIndication.this.plugin.getVanishStateMgr()
                                        .isVanished(infoData.getUserProfile().getUUID())
                                        && !receiver.getUniqueId().equals(infoData.getUserProfile().getUUID())
                                        && infoData.getGameMode() != GameMode.SPECTATOR) {
                                            int latency = infoData.getPing();
                                            if (packet.getAction() != WrapperPlayServerPlayerInfo.Action.UPDATE_GAME_MODE) {
                                                continue;
                                            }
                                            WrapperPlayServerPlayerInfo.PlayerData newData = new WrapperPlayServerPlayerInfo.PlayerData(
                                                    infoData.getDisplayName(),
                                                    infoData.getUserProfile(),
                                                    GameMode.SPECTATOR,
                                                    latency
                                                    );

                                            infoDataList.remove(infoData);
                                            infoDataList.add(newData);
                                }
                            }

                            packet.setPlayerDataList(infoDataList);
                        } catch (Exception | NoClassDefFoundError ex) {
                            if (!suppressErrors) {
                                VanishIndication.this.plugin.logException(ex);
                                plugin.getLogger().warning("IMPORTANT: Please make sure that you are using the latest " +
                                        "dev-build of packetevents and that your server is up-to-date! This error likely " +
                                        "happened inside of packetevents code which is out of SuperVanish's control. It's part " +
                                        "of an optional feature module and can be removed safely by disabling " +
                                        "MarkVanishedPlayersAsSpectators in the config file. Please report this " +
                                        "error if you can reproduce it on an up-to-date server with only latest " +
                                        "packetevents and latest SV installed.");
                                suppressErrors = true;
                            }
                        }
                    }
                }, PacketListenerPriority.NORMAL);
    }

    private void sendPlayerInfoChangeGameModePacket(Player p, Player change, boolean spectator) {
        UserProfile profile = new UserProfile(change.getUniqueId(), change.getName());

        GameMode gameMode = spectator ? GameMode.SPECTATOR : GameMode.valueOf(change.getGameMode().name());

        WrapperPlayServerPlayerInfo.PlayerData playerInfoData = new WrapperPlayServerPlayerInfo.PlayerData(
                Component.text(profile.getName()),
                profile,
                gameMode,
                change.getPing()
        );

        WrapperPlayServerPlayerInfo packet = new WrapperPlayServerPlayerInfo(
                WrapperPlayServerPlayerInfo.Action.UPDATE_GAME_MODE,
                Collections.singletonList(playerInfoData)
        );

        PacketEvents.getAPI().getProtocolManager().sendPacket(p, packet);
    }
}
