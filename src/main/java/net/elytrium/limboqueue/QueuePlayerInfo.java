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

package net.elytrium.limboqueue;

import net.elytrium.limboapi.api.player.LimboPlayer;

public class QueuePlayerInfo {
  
  private final LimboPlayer limboPlayer;
  private final long joinTime;
  private long waitingTime;
  
  public QueuePlayerInfo(LimboPlayer limboPlayer) {
    this.limboPlayer = limboPlayer;
    this.joinTime = System.currentTimeMillis();
    this.waitingTime = 0;
  }
  
  public LimboPlayer getLimboPlayer() {
    return this.limboPlayer;
  }
  
  public long getJoinTime() {
    return this.joinTime;
  }
  
  public long getWaitingTime() {
    return this.waitingTime;
  }
  
  public void updateWaitingTime() {
    this.waitingTime = (System.currentTimeMillis() - this.joinTime) / 1000;
  }
  
  public int getWaitingTimeSeconds() {
    return (int) this.waitingTime;
  }
}
