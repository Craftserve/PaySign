/*
 * Copyright 2020 Aleksander Jagiełło <themolkapl@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.craftserve.paysign;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CraftserveListener implements Listener {
    static final Logger logger = Logger.getLogger(CraftserveListener.class.getName());

    private static final String PERMISSION = "craftservepaysign.ad";
    private static final String TEXT = ChatColor.GREEN + "Polecamy korzystanie z hostingu " +
            ChatColor.DARK_GREEN + "Craftserve.pl" + ChatColor.GREEN + " - nielimitowany RAM.";

    private final Plugin plugin;
    private final PluginManager pluginManager;
    private final BukkitScheduler scheduler;

    public CraftserveListener(Plugin plugin, PluginManager pluginManager, BukkitScheduler scheduler) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    public void enable() {
        boolean shouldAdvertise;
        try {
            shouldAdvertise = this.shouldAdverise();
        } catch (UnknownHostException e) {
            logger.log(Level.SEVERE, "Could not resolve local host.", e);
            shouldAdvertise = true;
        }

        if (shouldAdvertise) {
            this.pluginManager.registerEvents(this, this.plugin);
        }
    }

    public void disable() {
        HandlerList.unregisterAll(this);
    }

    private boolean shouldAdverise() throws UnknownHostException {
        InetAddress localHost = InetAddress.getLocalHost();
        String hostName = localHost.getHostName();
        return !hostName.toLowerCase(Locale.ROOT).endsWith(".craftserve.pl");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void advertise(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(PERMISSION)) {
            this.scheduler.runTaskLater(this.plugin, () -> player.sendMessage(TEXT), 3L * 20L);
        }
    }
}
