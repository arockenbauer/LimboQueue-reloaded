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

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import java.io.File;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.elytrium.commons.kyori.serialization.Serializer;
import net.elytrium.commons.kyori.serialization.Serializers;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboqueue.commands.LimboQueueCommand;
import net.elytrium.limboqueue.handler.QueueHandler;
import net.elytrium.limboqueue.listener.QueueListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.slf4j.Logger;

@Plugin(id = "limboqueue", name = "LimboQueue", version = "1.0.1", authors = {"skywatcher_2019", "hevav"})
public class LimboQueue {

  @Inject
  private static Logger LOGGER;
  private static Serializer SERIALIZER;
  private final ProxyServer server;
  private final File configFile;
  private final LimboFactory factory;
  private final List<QueuePlayerInfo> queuedPlayers = new ArrayList<>();
  private ServerManager serverManager;
  private Limbo queueServer;
  private int checkInterval;
  private ScheduledTask queueTask;
  private ScheduledTask pingTask;
  private ScheduledTask actionbarTask;

  @Inject
  public LimboQueue(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
    setLogger(logger);

    this.server = server;

    File dataDirectoryFile = dataDirectory.toFile();
    this.configFile = new File(dataDirectoryFile, "config.yml");

    this.factory = (LimboFactory) this.server.getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).orElseThrow();
  }

  private static void setLogger(Logger logger) {
    LOGGER = logger;
  }

  public static Serializer getSerializer() {
    return SERIALIZER;
  }

  private static void setSerializer(Serializer serializer) {
    SERIALIZER = serializer;
  }

  public Logger getLogger() {
    return LOGGER;
  }

  public ServerManager getServerManager() {
    return this.serverManager;
  }



  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    this.reload();
  }

  public void reload() {
    Config.IMP.reload(this.configFile);
    ComponentSerializer<Component, Component, String> serializer = Serializers.valueOf(Config.IMP.MAIN.SERIALIZER.toUpperCase(Locale.ROOT))
        .getSerializer();
    if (serializer == null) {
      LOGGER.warn("The specified serializer could not be founded, using default. (LEGACY_AMPERSAND)");
      setSerializer(new Serializer(Objects.requireNonNull(Serializers.LEGACY_AMPERSAND.getSerializer())));
    } else {
      setSerializer(new Serializer(serializer));
    }

    this.checkInterval = Config.IMP.MAIN.CHECK_INTERVAL;

    // Initialize managers
    this.serverManager = new ServerManager(this);

    VirtualWorld queueWorld = this.factory.createVirtualWorld(Dimension.valueOf(Config.IMP.MAIN.WORLD.DIMENSION), 0, 100, 0, (float) 90, (float) 0.0);
    this.queueServer = this.factory.createLimbo(queueWorld).setName("LimboQueue").setWorldTime(6000);
    this.server.getEventManager().register(this, new QueueListener(this));

    CommandManager manager = this.server.getCommandManager();
    manager.unregister("limboqueue");
    manager.register("limboqueue", new LimboQueueCommand(this), "lq", "queue");

    // Start all tasks
    this.startPingTask();
    
    // Do initial server check before starting queue processing
    this.getServer().getScheduler().buildTask(this, () -> {
      this.serverManager.checkServers();
      LOGGER.info("Initial server check completed. Starting queue processing...");
      this.startQueueTask();
      this.startActionbarTask();
    }).delay(1, TimeUnit.SECONDS).schedule();
  }

  public void queuePlayer(Player player) {
    // Don't queue players who are already connected to a server
    if (player.getCurrentServer().isPresent()) {
      LOGGER.debug("Player {} is already connected to server {}, not queueing", 
          player.getUsername(), 
          player.getCurrentServer().get().getServerInfo().getName());
      return;
    }
    
    LOGGER.info("Queueing player {} (Available servers: {}/{})", 
        player.getUsername(),
        this.serverManager.getAvailableServers().size(),
        this.serverManager.getTargetServers().size());
    this.queueServer.spawnPlayer(player, new QueueHandler(this));
  }

  public ProxyServer getServer() {
    return this.server;
  }

  public synchronized void addQueuedPlayer(QueuePlayerInfo playerInfo) {
    this.queuedPlayers.add(playerInfo);
  }

  public synchronized void removeQueuedPlayer(QueuePlayerInfo playerInfo) {
    this.queuedPlayers.remove(playerInfo);
  }

  public synchronized List<QueuePlayerInfo> getQueuedPlayers() {
    return new ArrayList<>(this.queuedPlayers);
  }

  private void startQueueTask() {
    if (this.queueTask != null) {
      this.queueTask.cancel();
    }
    this.queueTask = this.getServer().getScheduler().buildTask(this, () -> {
      List<QueuePlayerInfo> currentQueue = this.getQueuedPlayers();
      
      // Clean up disconnected players from queue
      currentQueue.removeIf(playerInfo -> {
        Player player = playerInfo.getLimboPlayer().getProxyPlayer();
        if (player == null || !player.isActive()) {
          this.removeQueuedPlayer(playerInfo);
          LOGGER.debug("Removed disconnected player from queue");
          return true;
        }
        return false;
      });
      
      // CRITICAL: Only process queue if we have players AND available servers
      if (!currentQueue.isEmpty()) {
        // Double check that servers are available before attempting any connection
        if (!this.serverManager.hasAvailableServers()) {
          LOGGER.debug("No servers available, {} players waiting in queue", currentQueue.size());
          return; // Exit early if no servers available
        }
        
        // Get available server - with additional validation
        Optional<RegisteredServer> availableServer = this.serverManager.getAvailableServer();
        if (!availableServer.isPresent()) {
          LOGGER.warn("No available server found despite hasAvailableServers() returning true - this should not happen");
          return; // Exit early if no server available
        }
        
        // Triple check: verify the server is still actually available before connecting
        RegisteredServer server = availableServer.get();
        if (!this.serverManager.getAvailableServers().contains(server)) {
          LOGGER.warn("Selected server {} is no longer in available servers list", server.getServerInfo().getName());
          return; // Exit early if server is no longer available
        }
        
        // Get first player in queue
        QueuePlayerInfo firstPlayer = currentQueue.get(0);
        if (firstPlayer.getLimboPlayer().getProxyPlayer() == null) {
          LOGGER.debug("First player in queue has no proxy player, removing from queue");
          this.removeQueuedPlayer(firstPlayer);
          return;
        }
        
        Player player = firstPlayer.getLimboPlayer().getProxyPlayer();
        
        // Check if player is already connected to a server
        if (player.getCurrentServer().isPresent()) {
          LOGGER.debug("Player {} is already connected to server {}, removing from queue", 
              player.getUsername(), 
              player.getCurrentServer().get().getServerInfo().getName());
          this.removeQueuedPlayer(firstPlayer);
          return;
        }
        
        LOGGER.info("Attempting to connect player {} to server {} (Available servers: {}/{})", 
            player.getUsername(), 
            server.getServerInfo().getName(),
            this.serverManager.getAvailableServers().size(),
            this.serverManager.getTargetServers().size());
        
        // Send connecting message
        String connectingMessage = MessageFormat.format(Config.IMP.MESSAGES.CONNECTING_TO_SERVER, server.getServerInfo().getName());
        player.sendMessage(SERIALIZER.deserialize(connectingMessage));
        
        // Send actionbar message
        if (Config.IMP.MAIN.ENABLE_ACTIONBAR) {
          player.sendActionBar(SERIALIZER.deserialize(Config.IMP.MESSAGES.ACTIONBAR_CONNECTING));
        }
        
        // Remove player from queue BEFORE attempting connection to prevent multiple attempts
        this.removeQueuedPlayer(firstPlayer);
        
        // Try to connect player with proper error handling
        player.createConnectionRequest(server).connect().whenComplete((result, throwable) -> {
          if (throwable != null || !result.isSuccessful()) {
            // Connection failed
            String errorMessage = throwable != null ? throwable.getMessage() : "Connection unsuccessful";
            LOGGER.warn("Failed to connect player {} to server {}: {}", 
                player.getUsername(), 
                server.getServerInfo().getName(), 
                errorMessage);
            
            // Mark server as unavailable immediately to prevent further attempts
            this.serverManager.markServerUnavailable(server);
            
            // Only put player back in queue if still connected to limbo AND proxy
            Player currentPlayer = firstPlayer.getLimboPlayer().getProxyPlayer();
            if (currentPlayer != null && currentPlayer.isActive() && !currentPlayer.getCurrentServer().isPresent()) {
              // Player is still connected to proxy but not to any server, and still in limbo
              // Check if there are any other servers that might become available
              if (this.serverManager.getTotalServerCount() > 1 
                  || errorMessage.contains("Connection refused") 
                  || errorMessage.contains("ConnectException")) {
                // Server is likely down, put player back in queue to wait
                this.addQueuedPlayer(firstPlayer);
                LOGGER.info("Player {} returned to queue, waiting for servers to become available", 
                    player.getUsername());
              } else {
                // Send error message to player but keep them in queue
                currentPlayer.sendMessage(SERIALIZER.deserialize(Config.IMP.MESSAGES.NO_SERVERS_AVAILABLE));
                this.addQueuedPlayer(firstPlayer);
              }
            } else {
              // Player is no longer connected or already connected to a server, don't re-queue
              LOGGER.debug("Player {} is no longer connected to limbo or already connected to server, not re-queueing", 
                  player.getUsername());
            }
          } else {
            // Connection successful, disconnect from limbo
            LOGGER.info("Successfully connected player {} to server {}", 
                player.getUsername(), server.getServerInfo().getName());
            firstPlayer.getLimboPlayer().disconnect();
          }
        });
      }
    }).repeat(this.checkInterval, TimeUnit.SECONDS).schedule();
  }

  private void startPingTask() {
    if (this.pingTask != null) {
      this.pingTask.cancel();
    }
    this.pingTask = this.getServer().getScheduler().buildTask(this, () -> {
      this.serverManager.checkServers();
    }).repeat(this.checkInterval, TimeUnit.SECONDS).schedule();
  }

  private void startActionbarTask() {
    if (this.actionbarTask != null) {
      this.actionbarTask.cancel();
    }
    
    if (!Config.IMP.MAIN.ENABLE_ACTIONBAR) {
      return;
    }
    
    this.actionbarTask = this.getServer().getScheduler().buildTask(this, () -> {
      List<QueuePlayerInfo> currentQueue = this.getQueuedPlayers();
      
      for (int i = 0; i < currentQueue.size(); i++) {
        QueuePlayerInfo playerInfo = currentQueue.get(i);
        playerInfo.updateWaitingTime();
        
        if (playerInfo.getLimboPlayer().getProxyPlayer() != null) {
          Player player = playerInfo.getLimboPlayer().getProxyPlayer();
          
          String actionbarMessage;
          if (this.serverManager.hasAvailableServers()) {
            if (i == 0) {
              // First player in queue - will be connected soon
              actionbarMessage = Config.IMP.MESSAGES.ACTIONBAR_CONNECTING;
            } else {
              // Other players in queue
              actionbarMessage = MessageFormat.format(Config.IMP.MESSAGES.ACTIONBAR_QUEUE, 
                  i + 1, playerInfo.getWaitingTimeSeconds());
            }
          } else {
            actionbarMessage = MessageFormat.format(Config.IMP.MESSAGES.ACTIONBAR_WAITING, 
                playerInfo.getWaitingTimeSeconds());
          }
          
          player.sendActionBar(SERIALIZER.deserialize(actionbarMessage));
        }
      }
    }).repeat(Config.IMP.MAIN.ACTIONBAR_INTERVAL, TimeUnit.SECONDS).schedule();
  }


}
