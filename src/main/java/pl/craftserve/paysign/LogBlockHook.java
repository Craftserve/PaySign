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

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.Consumer;
import de.diddiz.LogBlock.LogBlock;
import org.bukkit.Location;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogBlockHook {
    static final Logger logger = Logger.getLogger(LogBlockHook.class.getName());

    /**
     * Logs click action on the sign to the LogBlock {@link Consumer}.
     * @param player Who clicked
     * @param trigger Trigger state
     * @param fakeButton Fake button simulating redstone
     */
    public void logClick(Player player, Trigger trigger, Switch fakeButton) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(fakeButton, "fakeButton");

        Consumer consumer = this.getConsumer();
        if (consumer == null) {
            return;
        }

        Actor actor = Actor.actorFromEntity(player);
        Location location = trigger.getPaySign().getSign().getLocation();

        try {
            consumer.queueBlock(actor, location, this.switchOff(fakeButton), fakeButton);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "Could not log click to LogBlock.", e);
        }
    }

    private Consumer getConsumer() {
        LogBlock logBlock = LogBlock.getInstance();
        return logBlock == null ? null : logBlock.getConsumer();
    }

    private Switch switchOff(Switch button) {
        Objects.requireNonNull(button, "button");
        Switch off = (Switch) button.clone();
        off.setPowered(false);
        return off;
    }
}
