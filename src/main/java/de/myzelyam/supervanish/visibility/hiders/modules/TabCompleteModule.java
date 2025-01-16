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
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import de.myzelyam.supervanish.SuperVanish;
import de.myzelyam.supervanish.visibility.hiders.PlayerHider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TabCompleteModule implements PacketListener {
    private final PlayerHider hider;
    private final SuperVanish plugin;

    private boolean errorLogged = false;

    public TabCompleteModule(SuperVanish plugin, PlayerHider hider) {
        this.plugin = plugin;
        this.hider = hider;
    }

    public static void register(SuperVanish plugin, PlayerHider hider) {
        PacketEvents.getAPI().getEventManager().registerListener(new TabCompleteModule(plugin, hider), PacketListenerPriority.HIGH);
    }

    @Override
    public void onPacketSend(PacketSendEvent e) {
        if (e.getPacketType() != PacketType.Play.Server.TAB_COMPLETE) return;

        try {
            WrapperPlayServerTabComplete packet = new WrapperPlayServerTabComplete(e);
            List<WrapperPlayServerTabComplete.CommandMatch> suggestions = packet.getCommandMatches();

            boolean containsHiddenPlayer = false;
            Iterator<WrapperPlayServerTabComplete.CommandMatch> iterator = suggestions.iterator();
            while (iterator.hasNext()) {
                WrapperPlayServerTabComplete.CommandMatch suggestion = iterator.next();
                String completion = suggestion.getText();

                if (completion.contains("/")) continue;
                if (hider.isHidden(completion, e.getPlayer())) {
                    iterator.remove();
                    containsHiddenPlayer = true;
                }
            }

            if (containsHiddenPlayer) {
                packet.setCommandMatches(suggestions);
            }
        } catch (Exception | NoClassDefFoundError ex) {
            if (ex.getMessage() == null
                    || !ex.getMessage().endsWith("is not supported for temporary players.")) {
                if (errorLogged) return;
                plugin.logException(ex);
                plugin.getLogger().warning("IMPORTANT: Please make sure that you are using the latest " +
                        "dev-build of packetevents and that your server is up-to-date! This error likely " +
                        "happened inside of packetevents code which is out of SuperVanish's control. It's part " +
                        "of an optional invisibility module and can be removed safely by disabling " +
                        "ModifyTabCompletePackets in the config. Please report this " +
                        "error if you can reproduce it on an up-to-date server with only latest " +
                        "packetevents and latest SV installed.");
                errorLogged = true;
            }
        }
    }
}
