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

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Logger;

/**
 * Represents a PaySign sign.
 */
public class SignData {
    static final Logger logger = Logger.getLogger(SignData.class.getName());

    protected static final String NAMESPACE = "[PaySign]";
    protected static final ChatColor NAMESPACE_COLOR = ChatColor.DARK_GREEN;

    private final Sign sign;
    private final String playerName;
    private final double price;
    private final int delay;

    public SignData(Sign sign, String playerName, double price, int delay) {
        this.sign = Objects.requireNonNull(sign, "sign");
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.price = price;
        this.delay = delay;
    }

    public Sign getSign() {
        return this.sign;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public Optional<Player> getOwner(Server server) {
        Objects.requireNonNull(server, "server");
        return Optional.ofNullable(server.getPlayer(this.playerName));
    }

    public double getPrice() {
        return this.price;
    }

    public double getPrice(boolean allowDecimals) {
        return allowDecimals ? this.price : (int) this.price;
    }

    public OptionalInt getDelay() {
        return this.delay > 0 ? OptionalInt.of(this.delay) : OptionalInt.empty();
    }

    public boolean pay(Player player, MessageRenderer messageRenderer, Economy economy, boolean allowDecimals) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(messageRenderer, "messageRenderer");
        Objects.requireNonNull(economy, "economy");

        String worldName = player.getWorld().getName();
        double price = this.getPrice(allowDecimals);

        if (Double.compare(price, 0) == 0) {
            // sign free of charge
            logger.finer("The sign is free of charge.");
            return true;
        } else if (!economy.has(player, worldName, price)) {
            // price cannot be negative
            logger.fine("The player is too poor to use this sign.");
            player.sendMessage(messageRenderer.tooPoor());
            return false;
        }

        EconomyResponse withdraw = economy.withdrawPlayer(player, worldName, price);
        if (!withdraw.transactionSuccess()) {
            logger.fine("Could not withdraw player.");
            player.sendMessage(messageRenderer.error(withdraw.errorMessage));
            return false;
        }

        EconomyResponse deposit = economy.depositPlayer(this.playerName, worldName, price);
        if (!deposit.transactionSuccess()) {
            logger.warning("Could not deposit " + this.playerName + " player for sign at " + this.sign.getLocation());
            economy.depositPlayer(player, worldName, price);
            player.sendMessage(messageRenderer.cantDeposit());
            return false;
        }

        String formattedPrice = economy.format(withdraw.amount);
        player.sendMessage(messageRenderer.paid(formattedPrice, this.playerName));
        logger.info(player.getName() + " has paid " + formattedPrice + " for using " + this.playerName + "'s mechanism.");

        this.getOwner(player.getServer()).ifPresent(owner -> {
            owner.sendMessage(messageRenderer.notification(player.getName(), formattedPrice));
        });
        return true;
    }

    public BlockFace getFacing() {
        BlockData blockData = this.sign.getBlock().getBlockData();
        Material material = blockData.getMaterial();

        if (Tag.STANDING_SIGNS.isTagged(material)) {
            return BlockFace.UP;
        } else if (Tag.WALL_SIGNS.isTagged(material) && blockData instanceof Directional) {
            return ((Directional) blockData).getFacing();
        } else {
            throw new IllegalStateException("Invalid block material: " + material);
        }
    }

    public Block getBaseBlock() {
        return this.sign.getBlock().getRelative(this.getFacing().getOppositeFace());
    }
}
