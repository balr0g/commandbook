// $Id$
/*
 * Copyright (C) 2010, 2011 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.sk89q.commandbook;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.sk89q.minecraft.util.commands.CommandException;

/**
 * Utility methods for CommandBook, borrowed from Tetsuuuu (the plugin
 * for SK's server).
 * 
 * @author sk89q
 */
public class CommandBookUtil {
    /**
     * Replace color macros in a string. The macros are in the form of `[char]
     * where char represents the color. R is for red, Y is for yellow,
     * G is for green, C is for cyan, B is for blue, and P is for purple.
     * The uppercase versions of those are the darker shades, while the
     * lowercase versions are the lighter shades. For white, it's 'w', and
     * 0-2 are black, dark grey, and grey, respectively.
     * 
     * @param str
     * @return color-coded string
     */
    public static String replaceColorMacros(String str) {
        str = str.replace("`r", ChatColor.RED.toString());
        str = str.replace("`R", ChatColor.DARK_RED.toString());
        
        str = str.replace("`y", ChatColor.YELLOW.toString());
        str = str.replace("`Y", ChatColor.GOLD.toString());

        str = str.replace("`g", ChatColor.GREEN.toString());
        str = str.replace("`G", ChatColor.DARK_GREEN.toString());
        
        str = str.replace("`c", ChatColor.AQUA.toString());
        str = str.replace("`C", ChatColor.DARK_AQUA.toString());
        
        str = str.replace("`b", ChatColor.BLUE.toString());
        str = str.replace("`B", ChatColor.DARK_BLUE.toString());
        
        str = str.replace("`p", ChatColor.LIGHT_PURPLE.toString());
        str = str.replace("`P", ChatColor.DARK_PURPLE.toString());

        str = str.replace("`0", ChatColor.BLACK.toString());
        str = str.replace("`1", ChatColor.DARK_GRAY.toString());
        str = str.replace("`2", ChatColor.GRAY.toString());
        str = str.replace("`w", ChatColor.WHITE.toString());
        
        return str;
    }
    
    /**
     * Get the 24-hour time string for a given Minecraft time.
     * 
     * @param time
     * @return
     */
    public static String getTimeString(long time) {
        int hours = (int) ((time / 1000 + 8) % 24);
        int minutes = (int) (60 * (time % 1000) / 1000);
        return String.format("%02d:%02d (%d:%02d %s)",
                hours, minutes, (hours % 12) == 0 ? 12 : hours % 12, minutes,
                hours < 12 ? "am" : "pm");
    }
    
    /**
     * Send the online player list.
     * 
     * @param online
     * @param sender
     */
    public static void sendOnlineList(Player[] online, CommandSender sender) {
        StringBuilder out = new StringBuilder();
        
        // This applies mostly to the console, so there might be 0 players
        // online if that's the case!
        if (online.length == 0) {
            sender.sendMessage("0 players are online.");
            return;
        }
        
        out.append(ChatColor.GRAY + "Online (");
        out.append(ChatColor.GRAY + "" + online.length);
        out.append(ChatColor.GRAY + "): ");
        out.append(ChatColor.WHITE);
        
        // To keep track of commas
        boolean first = true;
        
        for (Player player : online) {
            if (!first) {
                out.append(", ");
            }
            
            out.append(player.getName());
            
            first = false;
        }
        
        sender.sendMessage(out.toString());
    }
    
    /**
     * Get the cardinal compass direction of a player.
     * 
     * @param player
     * @return
     */
    public static String getCardinalDirection(Player player) {
        double rot = (player.getLocation().getYaw() - 90) % 360;
        if (rot < 0) {
            rot += 360.0;
        }
        return getDirection(rot);
    }

    /**
     * Converts a rotation to a cardinal direction name.
     * 
     * @param rot
     * @return
     */
    private static String getDirection(double rot) {
        if (0 <= rot && rot < 22.5) {
            return "North";
        } else if (22.5 <= rot && rot < 67.5) {
            return "Northeast";
        } else if (67.5 <= rot && rot < 112.5) {
            return "East";
        } else if (112.5 <= rot && rot < 157.5) {
            return "Southeast";
        } else if (157.5 <= rot && rot < 202.5) {
            return "South";
        } else if (202.5 <= rot && rot < 247.5) {
            return "Southwest";
        } else if (247.5 <= rot && rot < 292.5) {
            return "West";
        } else if (292.5 <= rot && rot < 337.5) {
            return "Northwest";
        } else if (337.5 <= rot && rot < 360.0) {
            return "North";
        } else {
            return null;
        }
    }
    
    /**
     * Process an item give request.
     * 
     * @param sender
     * @param item
     * @param amt
     * @param targets
     * @param plugin
     * @param drop
     * @throws CommandException
     */
    public static void giveItem(CommandSender sender, ItemStack item, int amt,
            Iterable<Player> targets, CommandBookPlugin plugin, boolean drop)
            throws CommandException {
        
        boolean included = false; // Is the command sender also receiving items?
        
        plugin.checkAllowedItem(sender, item.getTypeId());
        
        // Check for invalid amounts
        if (amt == 0 || amt < -1) {
            throw new CommandException("Invalid item amount!");
        } else if (amt == -1) {
            // Check to see if the player can give infinite items
            plugin.checkPermission(sender, "commandbook.give.infinite");
        } else if (amt > 64) {
            // Check to see if the player can give stacks
            plugin.checkPermission(sender, "commandbook.give.stacks");
        } else if (amt > 64 * 5) {
            plugin.checkPermission(sender, "commandbook.give.stacks.unlimited");
            
            // Check to see if the player can give stacks of this size
            throw new CommandException("More than 5 stacks is too excessive.");
        }
        
        // Get a nice amount name
        String amtText = amt == -1 ? "an infinite stack of" : String.valueOf(amt);
        
        for (Player player : targets) {
            int left = amt;
            
            // Give individual stacks
            while (left > 0 || amt == -1) {
                int givenAmt = Math.min(64, left);
                item.setAmount(givenAmt);
                left -= givenAmt;
                
                // The -d flag drops the items naturally on the ground instead
                // of directly giving the player the item
                if (drop) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                } else {
                    player.getInventory().addItem(item);
                }
                
                if (amt == -1) {
                    break;
                }
            }
            
            // Tell the user about the given item
            if (player.equals(sender)) {
                player.sendMessage(ChatColor.YELLOW + "You've been given " + amtText + " "
                        + plugin.toItemName(item.getTypeId()) + ".");
                
                // Keep track of this
                included = true;
            } else {
                player.sendMessage(ChatColor.YELLOW + "Given from "
                        + plugin.toName(sender) + ": "
                        + amtText + " "
                        + plugin.toItemName(item.getTypeId()) + ".");
                
            }
        }
        
        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included) {
            sender.sendMessage(ChatColor.YELLOW.toString() + amtText + " "
                    + plugin.toItemName(item.getTypeId()) + " has been given.");
        }
    }
}