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

package net.elytrium.limboqueue.handler;


import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboqueue.LimboQueue;
import net.elytrium.limboqueue.QueuePlayerInfo;

public class QueueHandler implements LimboSessionHandler {

  private final LimboQueue plugin;
  private LimboPlayer player;
  private QueuePlayerInfo playerInfo;

  public QueueHandler(LimboQueue plugin) {
    this.plugin = plugin;
  }

  @Override
  public void onSpawn(Limbo server, LimboPlayer player) {
    this.player = player;
    this.player.disableFalling();
    this.playerInfo = new QueuePlayerInfo(player);
    this.plugin.addQueuedPlayer(this.playerInfo);
    
    // Log player joining queue
    if (player.getProxyPlayer() != null) {
      this.plugin.getLogger().info("Player {} joined the queue", player.getProxyPlayer().getUsername());
    }
  }

  @Override
  public void onDisconnect() {
    if (this.playerInfo != null) {
      this.plugin.removeQueuedPlayer(this.playerInfo);
    }
  }
}
