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

import net.elytrium.commons.config.YamlConfig;

public class Config extends YamlConfig {

  @Ignore
  public static final Config IMP = new Config();

  @Create
  public MAIN MAIN;
  @Create
  public MESSAGES MESSAGES;

  public static class MAIN {

    @Comment("Serializers: LEGACY_AMPERSAND, LEGACY_SECTION, MINIMESSAGE")
    public String SERIALIZER = "MINIMESSAGE";
    @Comment("Server from velocity.toml which will checked for online")
    public String SERVER = "lobby";
    @Comment("Servers from velocity.toml which will be checked for availability (comma separated)")
    public String SERVERS = "lobby,lobby2";
    @Comment("Send player to the queue if kick reason contains this text (like \"The server if full!\")")
    public String KICK_MESSAGE = "The server is full";
    @Comment("Server checking interval in seconds")
    public int CHECK_INTERVAL = 2;
    @Comment("Enable automatic queue on join (players will be sent to queue world on join)")
    public boolean QUEUE_ON_JOIN = true;
    @Comment("Enable actionbar messages while in queue")
    public boolean ENABLE_ACTIONBAR = true;
    @Comment("Enable scoreboard while in queue")
    public boolean ENABLE_SCOREBOARD = false;
    @Comment("Actionbar update interval in seconds")
    public int ACTIONBAR_INTERVAL = 1;

    @Create
    public Config.MAIN.WORLD WORLD;

    public static class WORLD {

      @Comment("Dimensions: OVERWORLD, NETHER, THE_END")
      public String DIMENSION = "OVERWORLD";
    }

  }

  public static class MESSAGES {

    public String QUEUE_MESSAGE = "Your position in queue: {0}";
    public String SERVER_OFFLINE = "<red>Server is offline.";
    public String RELOAD = "<green>LimboQueue reloaded!";
    public String RELOAD_FAILED = "<red>Reload failed!";
    
    @Comment("Actionbar message when waiting for servers")
    public String ACTIONBAR_WAITING = "<yellow>⏳ Searching for available servers... <gray>({0}s)";
    @Comment("Actionbar message when server found")
    public String ACTIONBAR_CONNECTING = "<green>✓ Server found! Connecting...";
    @Comment("Actionbar message showing queue position")
    public String ACTIONBAR_QUEUE = "<aqua>Queue Position: <white>{0} <gray>| <yellow>Waiting time: <white>{1}s";
    
    @Comment("Message when no servers are available")
    public String NO_SERVERS_AVAILABLE = "<red>No servers are currently available. Please wait...";
    @Comment("Message when connecting to server")
    public String CONNECTING_TO_SERVER = "<green>Connecting to {0}...";
  }
}
