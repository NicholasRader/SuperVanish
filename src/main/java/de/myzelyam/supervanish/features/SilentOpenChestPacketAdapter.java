/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.features;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This is currently unused on Minecraft 1.19 or higher
 */
public class SilentOpenChestPacketAdapter implements PacketListener {

    private final SilentOpenChest silentOpenChest;

    private boolean suppressErrors = false;

    public SilentOpenChestPacketAdapter(SilentOpenChest silentOpenChest) {
        this.silentOpenChest = silentOpenChest;
    }

    @Override
    public void onPacketSend(PacketSendEvent e) {
        try {
            Player receiver = e.getPlayer();
            if (receiver == null) return;

            if (e.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
                WrapperPlayServerPlayerInfo packet = new WrapperPlayServerPlayerInfo(e);
                List<WrapperPlayServerPlayerInfo.PlayerData> infoDataList = new ArrayList<>(packet.getPlayerDataList());

                for (WrapperPlayServerPlayerInfo.PlayerData infoData : ImmutableList.copyOf(infoDataList)) {
                    UUID uuid = infoData.getUserProfile().getUUID();
                    if (!silentOpenChest.plugin.getVisibilityChanger().getHider().isHidden(uuid, receiver)
                            && silentOpenChest.plugin.getVanishStateMgr().isVanished(uuid)) {
                        Player vanishedTabPlayer = Bukkit.getPlayer(uuid);
                        if (infoData.getGameMode() == GameMode.SPECTATOR
                                && silentOpenChest.hasSilentlyOpenedChest(vanishedTabPlayer)
                                && packet.getAction() == WrapperPlayServerPlayerInfo.Action.UPDATE_GAME_MODE) {
                            int latency = infoData.getPing();
                            WrapperPlayServerPlayerInfo.PlayerData newData = new WrapperPlayServerPlayerInfo.PlayerData(
                                    infoData.getDisplayName(),
                                    infoData.getUserProfile(),
                                    GameMode.SURVIVAL,
                                    latency);
                            infoDataList.remove(infoData);
                            infoDataList.add(newData);
                        }
                    }
                }

                packet.setPlayerDataList(infoDataList);
            } else if (e.getPacketType() == PacketType.Play.Server.CHANGE_GAME_STATE) {
                if (silentOpenChest.plugin.getVanishStateMgr().isVanished(receiver.getUniqueId())) {
                    if (!silentOpenChest.hasSilentlyOpenedChest(receiver)) return;
                    e.setCancelled(true);
                }
            } else if (e.getPacketType() == PacketType.Play.Server.PLAYER_ABILITIES) {
                if (silentOpenChest.plugin.getVanishStateMgr().isVanished(receiver.getUniqueId())) {
                    if (!silentOpenChest.hasSilentlyOpenedChest(receiver)) return;
                    e.setCancelled(true);
                }
            } else if (e.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
                WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(e);
                int entityID = packet.getEntityId();
                if (entityID == receiver.getEntityId()) {
                    if (silentOpenChest.plugin.getVanishStateMgr().isVanished(receiver.getUniqueId())) {
                        if (!silentOpenChest.hasSilentlyOpenedChest(receiver)) return;
                        e.setCancelled(true);
                    }
                }
            }
        } catch (Exception | NoClassDefFoundError ex) {
            if (!suppressErrors) {
                if (ex.getMessage() == null
                        || !ex.getMessage().endsWith("is not supported for temporary players.")) {
                    silentOpenChest.plugin.logException(ex);
                    silentOpenChest.plugin.getLogger().warning("IMPORTANT: Please make sure that you are using the latest " +
                            "dev-build of packetevents and that your server is up-to-date! This error likely " +
                            "happened inside of packetevents code which is out of SuperVanish's control. It's part " +
                            "of an optional feature module and can be removed safely by disabling " +
                            "OpenChestsSilently in the config file. Please report this " +
                            "error if you can reproduce it on an up-to-date server with only latest " +
                            "packetevents and latest SV installed.");
                    suppressErrors = true;
                }
            }
        }
    }
}
