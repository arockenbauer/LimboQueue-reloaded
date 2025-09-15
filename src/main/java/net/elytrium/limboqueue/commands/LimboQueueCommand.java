/*
 * Copyright (C) 2022 - 2023 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboqueue.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import java.util.List;
import net.elytrium.commons.kyori.serialization.Serializer;
import net.elytrium.limboqueue.Config;
import net.elytrium.limboqueue.LimboQueue;
import net.elytrium.limboqueue.QueuePlayerInfo;
import net.kyori.adventure.text.Component;

public class LimboQueueCommand implements SimpleCommand {

  private final LimboQueue plugin;
  private final Component reload;
  private final Component reloadFailed;

  public LimboQueueCommand(LimboQueue plugin) {
    this.plugin = plugin;
    Serializer serializer = LimboQueue.getSerializer();
    this.reload = serializer.deserialize(Config.IMP.MESSAGES.RELOAD);
    this.reloadFailed = serializer.deserialize(Config.IMP.MESSAGES.RELOAD_FAILED);
  }

  @Override
  public List<String> suggest(Invocation invocation) {
    String[] args = invocation.arguments();

    if (args.length == 0) {
      return ImmutableList.of("reload", "status", "queue");
    } else {
      return ImmutableList.of();
    }
  }

  @Override
  public void execute(Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();

    if (args.length == 1) {
      String command = args[0];
      
      if (command.equalsIgnoreCase("reload") && source.hasPermission("limboqueue.reload")) {
        try {
          this.plugin.reload();
          source.sendMessage(this.reload);
        } catch (Exception e) {
          e.printStackTrace();
          source.sendMessage(this.reloadFailed);
        }
      } else if (command.equalsIgnoreCase("status") && source.hasPermission("limboqueue.status")) {
        this.showStatus(source);
      } else if (command.equalsIgnoreCase("queue") && source instanceof Player) {
        Player player = (Player) source;
        this.plugin.queuePlayer(player);
        player.sendMessage(LimboQueue.getSerializer().deserialize("<green>You have been added to the queue!"));
      }
    } else if (args.length == 0) {
      this.showHelp(source);
    }
  }

  private void showStatus(CommandSource source) {
    Serializer serializer = LimboQueue.getSerializer();
    List<QueuePlayerInfo> queuedPlayers = this.plugin.getQueuedPlayers();
    
    source.sendMessage(serializer.deserialize("<yellow>===== LimboQueue Status ====="));
    source.sendMessage(serializer.deserialize("<aqua>Players in queue: <white>" + queuedPlayers.size()));
    source.sendMessage(serializer.deserialize("<aqua>Available servers: <white>" 
        + this.plugin.getServerManager().getAvailableServerCount() + "/" 
        + this.plugin.getServerManager().getTotalServerCount()));
    
    if (!queuedPlayers.isEmpty()) {
      source.sendMessage(serializer.deserialize("<yellow>Queue:"));
      for (int i = 0; i < Math.min(queuedPlayers.size(), 10); i++) {
        QueuePlayerInfo playerInfo = queuedPlayers.get(i);
        playerInfo.updateWaitingTime();
        String playerName = playerInfo.getLimboPlayer().getProxyPlayer() != null 
            ? playerInfo.getLimboPlayer().getProxyPlayer().getUsername() : "Unknown";
        source.sendMessage(serializer.deserialize("<gray>" + (i + 1) + ". <white>" + playerName 
            + " <gray>(" + playerInfo.getWaitingTimeSeconds() + "s)"));
      }
      if (queuedPlayers.size() > 10) {
        source.sendMessage(serializer.deserialize("<gray>... and " + (queuedPlayers.size() - 10) + " more"));
      }
    }
  }

  private void showHelp(CommandSource source) {
    Serializer serializer = LimboQueue.getSerializer();
    source.sendMessage(serializer.deserialize("<yellow>===== LimboQueue Commands ====="));
    source.sendMessage(serializer.deserialize("<aqua>/limboqueue reload <gray>- Reload the plugin configuration"));
    source.sendMessage(serializer.deserialize("<aqua>/limboqueue status <gray>- Show queue status"));
    source.sendMessage(serializer.deserialize("<aqua>/limboqueue queue <gray>- Join the queue manually"));
  }
}
