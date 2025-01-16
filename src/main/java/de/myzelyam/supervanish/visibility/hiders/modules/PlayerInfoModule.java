/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * license, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.visibility.hiders.modules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.google.common.collect.ImmutableList;
import de.myzelyam.supervanish.SuperVanish;
import de.myzelyam.supervanish.visibility.hiders.PlayerHider;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * This is currently unused on Minecraft 1.19 or higher
 */
public class PlayerInfoModule implements PacketListener {

    private final PlayerHider hider;
    private final SuperVanish plugin;

    private boolean errorLogged = false;

    public PlayerInfoModule(SuperVanish plugin, PlayerHider hider) {
        this.plugin = plugin;
        this.hider = hider;
    }

    public static void register(SuperVanish plugin, PlayerHider hider) {
        PacketEvents.getAPI().getEventManager().registerListener(new PlayerInfoModule(plugin, hider), PacketListenerPriority.HIGH);
    }

    @Override
    public void onPacketSend(PacketSendEvent e) {
        if (e.getPacketType() != PacketType.Play.Server.PLAYER_INFO) {
            return;
        }

        try {
            WrapperPlayServerPlayerInfo packet = new WrapperPlayServerPlayerInfo(e);
            List<WrapperPlayServerPlayerInfo.PlayerData> infoDataList = new ArrayList<>(packet.getPlayerDataList());

            if (infoDataList.isEmpty()) {
                e.setCancelled(true);
                return;
            }

            Player receiver = e.getPlayer();
            for (WrapperPlayServerPlayerInfo.PlayerData infoData : ImmutableList.copyOf(infoDataList)) {
                if (hider.isHidden(infoData.getUserProfile().getUUID(), receiver)) {
                    infoDataList.remove(infoData);
                }
            }

            packet.setPlayerDataList(infoDataList);
        } catch (Exception | NoClassDefFoundError ex) {
            if (ex.getMessage() == null
                    || !ex.getMessage().endsWith("is not supported for temporary players.")) {
                if (errorLogged) return;
                plugin.logException(ex);
                plugin.getLogger().warning("IMPORTANT: Please make sure that you are using the latest " +
                        "dev-build of packetevents and that your server is up-to-date! This error likely " +
                        "happened inside of packetevents code which is out of SuperVanish's control. It's part " +
                        "of an optional invisibility module and can be removed safely by disabling " +
                        "ModifyTablistPackets in the config. Please report this " +
                        "error if you can reproduce it on an up-to-date server with only latest " +
                        "packetevents and latest SV installed.");
                errorLogged = true;
            }
        }
    }
}
