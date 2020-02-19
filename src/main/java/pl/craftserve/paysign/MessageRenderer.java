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

import java.util.Objects;

/**
 * Renders different messages.
 */
public abstract class MessageRenderer {
    public String cantDeposit() {
        return this.error("Could not deposit target player.");
    }

    public String createdSuccessfully() {
        return this.success("Sign has been created.");
    }

    public String disabledDecimals() {
        return this.error("Decimal prices aren't allowed on this server.");
    }

    public String noPermissionToCreate() {
        return this.error("You don't have permission to create this sign.");
    }

    public String noPermissionToUse() {
        return this.error("You don't have permission to use this sign.");
    }

    public String notification(String playerName, String formattedPrice) {
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(formattedPrice, "formattedPrice");
        return this.fine(playerName + " has paid " + formattedPrice + " for using your mechanism.");
    }

    public String paid(String formattedPrice, String ownerName) {
        Objects.requireNonNull(formattedPrice, "formattedPrice");
        Objects.requireNonNull(ownerName, "ownerName");
        return this.success(formattedPrice + " has been withdrawn from your account for using " + ownerName + "'s mechanism.");
    }

    public String tooPoor() {
        return this.error("You are too poor to use this sign.");
    }

    //
    // Formatters
    //

    public String error(String text) {
        return this.colored(text, ChatColor.RED);
    }

    private String success(String text) {
        return this.colored(text, ChatColor.GREEN);
    }

    private String fine(String text) {
        return this.colored(text, ChatColor.GRAY);
    }

    private String colored(String text, ChatColor color) {
        Objects.requireNonNull(color, "color");
        return this.prefixed(color.toString() + text);
    }

    public abstract String prefixed(String text);
}
