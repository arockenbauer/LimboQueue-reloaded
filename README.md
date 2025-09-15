# LimboQueue

LimboQueue est un plugin de file d'attente pour [Velocity](https://velocitypowered.com) construit avec [LimboAPI](https://github.com/elytrium/limboapi). Ce plugin est un fork amélioré du LimboQueue original, conçu pour faire patienter les joueurs dans un monde limbo virtuel pendant qu'ils attendent qu'un serveur de lobby soit disponible.

## Description

LimboQueue résout le problème des serveurs pleins ou temporairement indisponibles en plaçant automatiquement les joueurs dans une file d'attente virtuelle. Au lieu de recevoir un message d'erreur ou d'être déconnectés, les joueurs sont transportés dans un monde limbo où ils peuvent attendre confortablement qu'une place se libère sur les serveurs de destination.

### Fonctionnalités principales

- **File d'attente automatique** : Les joueurs sont automatiquement placés en file d'attente lors de la connexion si les serveurs sont pleins ou indisponibles
- **Monde limbo virtuel** : Un environnement d'attente personnalisable où les joueurs patientent
- **Surveillance multi-serveurs** : Vérifie la disponibilité de plusieurs serveurs simultanément
- **Messages informatifs** : ActionBar dynamique qui informe les joueurs de leur position et du temps d'attente
- **Connexion intelligente** : Connecte automatiquement les joueurs dès qu'un serveur devient disponible
- **Gestion des déconnexions** : Nettoie automatiquement la file d'attente des joueurs déconnectés

## Installation

### Prérequis
- Velocity (dernière version)
- LimboAPI (dernière version)
- Java 11 ou supérieur

### Étapes d'installation
1. Téléchargez le fichier JAR depuis les releases ou compilez le projet
2. Placez le fichier `LimboQueue-1.0.1.jar` dans le dossier `plugins` de votre serveur Velocity
3. Redémarrez votre serveur Velocity
4. Le plugin générera automatiquement un fichier de configuration `config.yml`

## Configuration

Le fichier de configuration se trouve dans `plugins/limboqueue/config.yml` :

```yaml
main:
  # Format des messages : LEGACY_AMPERSAND, LEGACY_SECTION, MINIMESSAGE
  serializer: "MINIMESSAGE"
  
  # Serveur principal à vérifier (défini dans velocity.toml)
  server: "lobby"
  
  # Liste des serveurs à surveiller (séparés par des virgules)
  servers: "lobby,lobby2"
  
  # Message de kick qui déclenche la mise en file d'attente
  kick-message: "The server is full"
  
  # Intervalle de vérification des serveurs (en secondes)
  check-interval: 2
  
  # Activer la file d'attente automatique à la connexion
  queue-on-join: true
  
  # Activer les messages dans la barre d'action
  enable-actionbar: true
  
  # Activer le scoreboard (désactivé par défaut)
  enable-scoreboard: false
  
  # Intervalle de mise à jour de l'actionbar (en secondes)
  actionbar-interval: 1
  
  world:
    # Dimension du monde limbo : OVERWORLD, NETHER, THE_END
    dimension: "OVERWORLD"

messages:
  # Messages personnalisables
  queue-message: "Your position in queue: {0}"
  server-offline: "<red>Server is offline."
  reload: "<green>LimboQueue reloaded!"
  reload-failed: "<red>Reload failed!"
  
  # Messages de la barre d'action
  actionbar-waiting: "<yellow>⏳ Searching for available servers... <gray>({0}s)"
  actionbar-connecting: "<green>✓ Server found! Connecting..."
  actionbar-queue: "<aqua>Queue Position: <white>{0} <gray>| <yellow>Waiting time: <white>{1}s"
  
  # Messages d'état
  no-servers-available: "<red>No servers are currently available. Please wait..."
  connecting-to-server: "<green>Connecting to {0}..."
```

## Commandes et permissions

### Commandes administrateur
- `/limboqueue reload` ou `/lq reload` - Recharge la configuration du plugin
  - Permission : `limboqueue.reload`

### Commandes joueur
- `/limboqueue queue` ou `/lq queue` - Rejoint manuellement la file d'attente
- `/limboqueue status` ou `/lq status` - Affiche l'état des serveurs surveillés

## Fonctionnement

### Flux automatique
1. **Connexion du joueur** : Le joueur se connecte au proxy Velocity
2. **Vérification automatique** : Si `queue-on-join` est activé, le plugin vérifie la disponibilité des serveurs
3. **Mise en file d'attente** : Si aucun serveur n'est disponible, le joueur est placé dans le monde limbo
4. **Attente interactive** : Le joueur voit sa position dans la file et le temps d'attente via l'ActionBar
5. **Connexion automatique** : Dès qu'un serveur devient disponible, le joueur y est automatiquement connecté

### Gestion des serveurs pleins
Lorsqu'un joueur est expulsé d'un serveur avec un message contenant le texte configuré dans `kick-message`, il est automatiquement placé en file d'attente au lieu d'être déconnecté du proxy.

## Avantages de ce fork

Cette version améliorée du LimboQueue original apporte plusieurs corrections et améliorations :

- **Stabilité accrue** : Résolution des problèmes de configuration et de sérialisation
- **Gestion intelligente des erreurs** : Meilleure gestion des déconnexions et des serveurs indisponibles
- **Interface utilisateur améliorée** : Messages plus clairs et informatifs
- **Configuration simplifiée** : Suppression des éléments problématiques tout en conservant les fonctionnalités essentielles
- **Surveillance robuste** : Vérification continue de l'état des serveurs avec reconnexion automatique

## Support et contribution

Ce plugin est un fork maintenu de LimboQueue. Pour signaler des problèmes ou contribuer au développement, veuillez utiliser le système d'issues du repository.

## Licence

Ce projet est distribué sous licence GPL v3. Voir le fichier `LICENSE` pour plus de détails.

---

# LimboQueue (English Version)

LimboQueue is a queue plugin for [Velocity](https://velocitypowered.com) built with [LimboAPI](https://github.com/elytrium/limboapi). This plugin is an improved fork of the original LimboQueue, designed to make players wait in a virtual limbo world while they wait for a lobby server to become available.

## Description

LimboQueue solves the problem of full or temporarily unavailable servers by automatically placing players in a virtual queue. Instead of receiving an error message or being disconnected, players are transported to a limbo world where they can wait comfortably for a spot to open up on the destination servers.

### Main Features

- **Automatic queue** : Players are automatically placed in queue upon connection if servers are full or unavailable
- **Virtual limbo world** : A customizable waiting environment where players wait
- **Multi-server monitoring** : Checks the availability of multiple servers simultaneously
- **Informative messages** : Dynamic ActionBar that informs players of their position and waiting time
- **Smart connection** : Automatically connects players as soon as a server becomes available
- **Disconnection management** : Automatically cleans the queue of disconnected players

## Installation

### Prerequisites
- Velocity (latest version)
- LimboAPI (latest version)
- Java 11 or higher

### Installation Steps
1. Download the JAR file from releases or compile the project
2. Place the `LimboQueue-1.0.1.jar` file in your Velocity server's `plugins` folder
3. Restart your Velocity server
4. The plugin will automatically generate a `config.yml` configuration file

## Configuration

The configuration file is located in `plugins/limboqueue/config.yml`:

```yaml
main:
  # Message format: LEGACY_AMPERSAND, LEGACY_SECTION, MINIMESSAGE
  serializer: "MINIMESSAGE"
  
  # Main server to check (defined in velocity.toml)
  server: "lobby"
  
  # List of servers to monitor (comma separated)
  servers: "lobby,lobby2"
  
  # Kick message that triggers queueing
  kick-message: "The server is full"
  
  # Server check interval (in seconds)
  check-interval: 2
  
  # Enable automatic queue on join
  queue-on-join: true
  
  # Enable actionbar messages
  enable-actionbar: true
  
  # Enable scoreboard (disabled by default)
  enable-scoreboard: false
  
  # Actionbar update interval (in seconds)
  actionbar-interval: 1
  
  world:
    # Limbo world dimension: OVERWORLD, NETHER, THE_END
    dimension: "OVERWORLD"

messages:
  # Customizable messages
  queue-message: "Your position in queue: {0}"
  server-offline: "<red>Server is offline."
  reload: "<green>LimboQueue reloaded!"
  reload-failed: "<red>Reload failed!"
  
  # ActionBar messages
  actionbar-waiting: "<yellow>Searching for available servers... <gray>({0}s)"
  actionbar-connecting: "<green>Server found! Connecting..."
  actionbar-queue: "<aqua>Queue Position: <white>{0} <gray>| <yellow>Waiting time: <white>{1}s"
  
  # Status messages
  no-servers-available: "<red>No servers are currently available. Please wait..."
  connecting-to-server: "<green>Connecting to {0}..."
```

## Commands and Permissions

### Administrator Commands
- `/limboqueue reload` or `/lq reload` - Reload plugin configuration
  - Permission: `limboqueue.reload`

### Player Commands
- `/limboqueue queue` or `/lq queue` - Manually join the queue
- `/limboqueue status` or `/lq status` - Display the status of monitored servers

## How It Works

### Automatic Flow
1. **Player connection** : Player connects to the Velocity proxy
2. **Automatic check** : If `queue-on-join` is enabled, the plugin checks server availability
3. **Queue placement** : If no server is available, the player is placed in the limbo world
4. **Interactive waiting** : Player sees their position in queue and waiting time via ActionBar
5. **Automatic connection** : As soon as a server becomes available, the player is automatically connected

### Full Server Management
When a player is kicked from a server with a message containing the text configured in `kick-message`, they are automatically placed in queue instead of being disconnected from the proxy.

## Advantages of This Fork

This improved version of the original LimboQueue brings several fixes and improvements:

- **Increased stability** : Resolution of configuration and serialization issues
- **Smart error handling** : Better management of disconnections and unavailable servers
- **Improved user interface** : Clearer and more informative messages
- **Simplified configuration** : Removal of problematic elements while preserving essential features
- **Robust monitoring** : Continuous server status checking with automatic reconnection

## Support and Contribution

This plugin is a maintained fork of LimboQueue. To report issues or contribute to development, please use the repository's issue system.

## License

This project is distributed under GPL v3 license. See the `LICENSE` file for more details.
