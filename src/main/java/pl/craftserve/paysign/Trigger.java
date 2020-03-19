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

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Switch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Triggers current for {@link PaySign}s.
 */
public class Trigger implements Listener, Predicate<Block> {
    static final Logger logger = Logger.getLogger(Trigger.class.getName());

    private static final Supplier<Switch> BUTTON_FACTORY = () -> (Switch) Material.OAK_BUTTON.createBlockData();
    private static final BlockFace FLOOR_FACING = BlockFace.NORTH;
    private static final Sound SOUND_ON = Sound.BLOCK_WOODEN_BUTTON_CLICK_ON;
    private static final Sound SOUND_OFF = Sound.BLOCK_WOODEN_BUTTON_CLICK_OFF;
    private static final float SOUND_VOLUME = .3F;

    private final Plugin plugin;
    private final PaySign paySign;
    private Block baseBlock;

    public Trigger(Plugin plugin, PaySign paySign) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.paySign = Objects.requireNonNull(paySign, "paySign");
    }

    @Override
    public boolean test(Block block) {
        if (this.paySign.getSign().getBlock().equals(block)) {
            return true;
        }
        return this.baseBlock != null && this.baseBlock.equals(block);
    }

    public PaySign getPaySign() {
        return this.paySign;
    }

    public Switch execute() {
        logger.finer("Registering events for trigger.");
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        this.baseBlock = this.paySign.getBaseBlock();

        Switch button = this.createFakeButton();
        this.paySign.getSign().getBlock().setBlockData(button);
        this.playSound(SOUND_ON, .6F);

        this.updateBaseBlockNeighbors();
        return button;
    }

    public Switch createFakeButton() {
        logger.fine("Creating fake button.");
        BlockFace facing = this.paySign.getFacing();
        Switch button = BUTTON_FACTORY.get();

        button.setFace(facing.equals(BlockFace.UP) ? Switch.Face.FLOOR : Switch.Face.WALL);
        button.setFacing(facing.equals(BlockFace.UP) ? FLOOR_FACING : facing);
        button.setPowered(true);
        return button;
    }

    private void playSound(Sound sound, float pitch) {
        Objects.requireNonNull(sound, "sound");
        Sign sign = this.paySign.getSign();
        sign.getWorld().playSound(sign.getLocation(), sound, SoundCategory.BLOCKS, SOUND_VOLUME, pitch);
    }

    private void updateBaseBlockNeighbors() {
        BlockData realBlockData = this.baseBlock.getBlockData();
        Material material = realBlockData.getMaterial().equals(Material.BARRIER)
                ? Material.STONE : Material.BARRIER;

        // Simulate block change to call World.applyPhysics on the base block.
        this.baseBlock.setBlockData(material.createBlockData(), false);
        this.baseBlock.setBlockData(realBlockData, true);
    }

    public void flush() {
        logger.fine("Restoring fake button back to the sign.");
        try {
            this.paySign.getSign().update(true, true);
            this.playSound(SOUND_OFF, .5F);
            this.updateBaseBlockNeighbors();
        } finally {
            HandlerList.unregisterAll(this);
            this.baseBlock = null;
        }
    }

    //
    // Blocking Listeners
    //

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelBlockBreak(BlockBreakEvent event) {
        if (this.test(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelBlockBurn(BlockBurnEvent event) {
        if (this.test(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelBlockFade(BlockFadeEvent event) {
        if (this.test(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelEntityChangeBlock(EntityChangeBlockEvent event) {
        if (this.test(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelPhysics(BlockPhysicsEvent event) {
        if (this.test(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (this.test(block)) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (this.test(block)) {
                event.setCancelled(true);
                break;
            }
        }
    }
}
