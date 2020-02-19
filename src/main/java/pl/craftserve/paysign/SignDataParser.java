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

import com.google.common.base.Preconditions;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;

import java.util.Objects;
import java.util.Optional;

/**
 * Parsers {@link SignData} from {@link Sign}s or directly from its lines.
 */
public class SignDataParser {
    public Optional<SignData> parse(Sign sign) throws ParseException {
        Objects.requireNonNull(sign, "sign");
        return this.parse(sign, sign.getLines());
    }

    public Optional<SignData> parse(Sign sign, String[] lines) throws ParseException {
        Objects.requireNonNull(sign, "sign");
        Objects.requireNonNull(lines, "lines");
        Preconditions.checkArgument(lines.length == 4, "4 lines expected, " + lines.length + " given");

        // identifier
        if (!ChatColor.stripColor(lines[0]).equals(SignData.NAMESPACE)) {
            return Optional.empty();
        }

        // player name
        String playerName = lines[1];
        if (playerName.isEmpty()) {
            throw new ParseException(1, "No player name given");
        }

        // price
        if (lines[2].isEmpty()) {
            throw new ParseException(2, "No price given");
        }

        double price;
        try {
            price = Double.parseDouble(lines[2]);
        } catch (NumberFormatException e) {
            throw new ParseException(2, "Price is not a number", e);
        }

        if (Double.compare(price, 0) < 0) {
            throw new ParseException(2, "Price cannot be negative");
        }

        // delay
        int delay = 0;
        if (!lines[3].isEmpty()) {
            try {
                delay = Integer.parseInt(lines[3]) * 20; // convert seconds to ticks
            } catch (NumberFormatException e) {
                throw new ParseException(3, "Redstone delay is not a number", e);
            }

            if (delay < 1) {
                throw new ParseException(3, "Redstone delay must be positive");
            }
        }

        return Optional.of(new SignData(sign, playerName, price, delay));
    }

    public static class ParseException extends Exception {
        private final int line;

        public ParseException(int line, String message) {
            super(message);
            this.line = line;
        }

        public ParseException(int line, String message, Throwable cause) {
            super(message, cause);
            this.line = line;
        }

        public int getLine() {
            return this.line;
        }

        public String getText() {
            StringBuilder text = new StringBuilder();
            text.append("Line ").append(this.line + 1).append(": ");
            text.append(this.getMessage());

            Throwable cause = this.getCause();
            if (cause != null) {
                String message = cause.getMessage();
                if (message != null) {
                    text.append(": ").append(message);
                }
            }

            return text.toString();
        }
    }
}
