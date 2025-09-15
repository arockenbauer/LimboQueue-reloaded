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

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;

public class ServerManager {
  
  private final LimboQueue plugin;
  private final List<RegisteredServer> targetServers;
  private final List<RegisteredServer> availableServers;
  private final Map<RegisteredServer, Long> unavailableServers;
  private int lastSelectedIndex = 0;
  
  public ServerManager(LimboQueue plugin) {
    this.plugin = plugin;
    this.targetServers = new ArrayList<>();
    this.availableServers = new ArrayList<>();
    this.unavailableServers = new HashMap<>();
    this.loadServers();
  }
  
  private void loadServers() {
    this.targetServers.clear();
    
    // Use SERVERS if it contains multiple servers, otherwise use SERVER for compatibility
    String serversConfig = Config.IMP.MAIN.SERVERS;
    if (serversConfig.contains(",")) {
      // Multiple servers configured
      String[] serverNames = serversConfig.split(",");
      for (String serverName : serverNames) {
        serverName = serverName.trim();
        Optional<RegisteredServer> server = this.plugin.getServer().getServer(serverName);
        if (server.isPresent()) {
          this.targetServers.add(server.get());
        } else {
          this.plugin.getLogger().warn("Server '" + serverName + "' not found in velocity.toml!");
        }
      }
    } else {
      // Single server - use SERVER for compatibility
      String serverName = Config.IMP.MAIN.SERVER.trim();
      Optional<RegisteredServer> server = this.plugin.getServer().getServer(serverName);
      if (server.isPresent()) {
        this.targetServers.add(server.get());
      } else {
        this.plugin.getLogger().warn("Server '" + serverName + "' not found in velocity.toml!");
      }
    }
    
    if (this.targetServers.isEmpty()) {
      this.plugin.getLogger().error("No valid servers found! Please check your configuration.");
    }
  }
  
  public void checkServers() {
    this.availableServers.clear();
    
    // Clean up servers that have been unavailable for more than 30 seconds
    long currentTime = System.currentTimeMillis();
    this.unavailableServers.entrySet().removeIf(entry -> 
        currentTime - entry.getValue() > 30000); // 30 seconds
    
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    
    for (RegisteredServer server : this.targetServers) {
      // Skip servers that are temporarily marked as unavailable
      if (this.unavailableServers.containsKey(server)) {
        this.plugin.getLogger().debug("Skipping server {} - temporarily marked as unavailable", 
            server.getServerInfo().getName());
        continue;
      }
      
      CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
        // First check if server is connectable via socket
        if (!this.isServerConnectable(server)) {
          this.plugin.getLogger().debug("Server {} is not connectable", 
              server.getServerInfo().getName());
          return null;
        }
        
        // If connectable, then check with ping for player count
        try {
          ServerPing ping = server.ping().orTimeout(5, TimeUnit.SECONDS).join();
          if (ping != null && ping.getPlayers().isPresent()) {
            ServerPing.Players players = ping.getPlayers().get();
            // Only consider server available if it has space for players
            if (players.getOnline() < players.getMax()) {
              synchronized (this.availableServers) {
                this.availableServers.add(server);
              }
              this.plugin.getLogger().debug("Server {} is available ({}/{} players)", 
                  server.getServerInfo().getName(), players.getOnline(), players.getMax());
            } else {
              this.plugin.getLogger().debug("Server {} is full ({}/{} players)", 
                  server.getServerInfo().getName(), players.getOnline(), players.getMax());
            }
          } else {
            // If connectable but no ping response, DO NOT assume it's available
            // Only add to available servers if we get a proper ping response
            this.plugin.getLogger().debug("Server {} is connectable but ping failed, NOT marking as available", 
                server.getServerInfo().getName());
          }
        } catch (Exception e) {
          this.plugin.getLogger().debug("Server {} ping failed after successful connection test: {}", 
              server.getServerInfo().getName(), e.getMessage());
          // If ping fails, DO NOT consider it available - we need a proper ping response
          // to ensure the server is actually ready to accept players
        }
        return null;
      });
      futures.add(future);
    }
    
    // Wait for all checks to complete
    try {
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
          .orTimeout(10, TimeUnit.SECONDS)
          .join();
    } catch (Exception e) {
      this.plugin.getLogger().warn("Server check timed out or failed: {}", e.getMessage());
    }
    
    this.plugin.getLogger().info("Server check completed: {}/{} servers available", 
        this.availableServers.size(), this.targetServers.size());
  }
  
  public Optional<RegisteredServer> getAvailableServer() {
    synchronized (this.availableServers) {
      if (this.availableServers.isEmpty()) {
        return Optional.empty();
      }
      
      // Use round-robin selection to avoid always selecting the same server
      if (this.lastSelectedIndex >= this.availableServers.size()) {
        this.lastSelectedIndex = 0;
      }
      
      RegisteredServer selectedServer = this.availableServers.get(this.lastSelectedIndex);
      this.lastSelectedIndex = (this.lastSelectedIndex + 1) % this.availableServers.size();
      
      return Optional.of(selectedServer);
    }
  }
  
  public List<RegisteredServer> getTargetServers() {
    return new ArrayList<>(this.targetServers);
  }
  
  public List<RegisteredServer> getAvailableServers() {
    synchronized (this.availableServers) {
      return new ArrayList<>(this.availableServers);
    }
  }
  
  public int getAvailableServerCount() {
    return this.availableServers.size();
  }
  
  public int getTotalServerCount() {
    return this.targetServers.size();
  }
  
  public boolean hasAvailableServers() {
    return !this.availableServers.isEmpty();
  }
  
  public void reload() {
    this.loadServers();
  }
  
  public void markServerUnavailable(RegisteredServer server) {
    synchronized (this.availableServers) {
      this.availableServers.remove(server);
    }
    // Mark server as temporarily unavailable for 30 seconds
    this.unavailableServers.put(server, System.currentTimeMillis());
    this.plugin.getLogger().info("Marked server {} as temporarily unavailable", 
        server.getServerInfo().getName());
  }
  
  private boolean isServerConnectable(RegisteredServer server) {
    String address = server.getServerInfo().getAddress().getHostString();
    int port = server.getServerInfo().getAddress().getPort();
    
    try {
      Socket socket = SocketFactory.getDefault().createSocket();
      socket.setSoTimeout(2000); // 2 second timeout - faster detection
      socket.connect(new InetSocketAddress(address, port), 2000); // Connection timeout
      socket.close();
      this.plugin.getLogger().debug("Server {} socket connection successful", 
          server.getServerInfo().getName());
      return true;
    } catch (ConnectException e) {
      this.plugin.getLogger().debug("Server {} connection refused: {}", 
          server.getServerInfo().getName(), e.getMessage());
      return false;
    } catch (IOException e) {
      this.plugin.getLogger().debug("Server {} connection failed: {}", 
          server.getServerInfo().getName(), e.getMessage());
      return false;
    } catch (Exception e) {
      this.plugin.getLogger().debug("Server {} unexpected connection error: {}", 
          server.getServerInfo().getName(), e.getMessage());
      return false;
    }
  }
}
