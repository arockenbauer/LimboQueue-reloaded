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

package net.elytrium.limboqueue.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.elytrium.limboqueue.Config;
import net.elytrium.limboqueue.LimboQueue;

public class QueueListener {

  private final LimboQueue plugin;

  public QueueListener(LimboQueue plugin) {
    this.plugin = plugin;
  }

  @Subscribe(order = PostOrder.FIRST)
  public void onPostLogin(PostLoginEvent event) {
    // Don't automatically queue players on login
    // Let Velocity handle normal connections when servers are available
    // Only queue players when they get kicked or when connections fail
    this.plugin.getLogger().debug("Player {} logged in, letting Velocity handle initial connection", 
        event.getPlayer().getUsername());
  }

  @Subscribe(order = PostOrder.FIRST)
  public void onServerPreConnect(ServerPreConnectEvent event) {
    // Check if this is a connection to one of our target servers
    boolean isTargetServer = this.plugin.getServerManager().getTargetServers().stream()
        .anyMatch(server -> server.equals(event.getOriginalServer()));
    
    if (isTargetServer) {
      // Check if we have available servers
      if (!this.plugin.getServerManager().hasAvailableServers()) {
        // No servers available, cancel the connection and send to queue
        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        this.plugin.getLogger().info("Prevented connection to {} - no servers available, sending player to queue", 
            event.getOriginalServer().getServerInfo().getName());
        
        // Send player to queue
        this.plugin.queuePlayer(event.getPlayer());
        return;
      } else {
        // Servers are available, allow the connection
        this.plugin.getLogger().debug("Allowing connection to {} - servers are available", 
            event.getOriginalServer().getServerInfo().getName());
      }
    }
  }

  @Subscribe
  public void onLoginLimboRegister(LoginLimboRegisterEvent event) {
    event.setOnKickCallback((kickEvent) -> {
      // Check if the kick is from one of our target servers
      boolean isTargetServer = this.plugin.getServerManager().getTargetServers().stream()
          .anyMatch(server -> server.equals(kickEvent.getServer()));
      
      if (!isTargetServer) {
        return false;
      }

      if (kickEvent.getServerKickReason().isEmpty()) {
        return false;
      }

      String reason = LimboQueue.getSerializer().serialize(kickEvent.getServerKickReason().get());
      if (reason.contains(Config.IMP.MAIN.KICK_MESSAGE)) {
        this.plugin.queuePlayer(kickEvent.getPlayer());
        return true;
      }
      return false;
    });
  }
}
