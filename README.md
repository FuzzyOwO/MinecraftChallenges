# MinecraftChallenges

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

---
Supported Minecraft version: 26.1.2

**Only ForceBattle is supported right now!**

---

## 📖 About ForceBattle

**ForceBattle** is a multiplayer gamemode where players compete to complete randomized objectives and earn points.  
The player with the most points after the timer ends wins the battle!

This is my updated version from the original one by nicolaigrv which is available [here](https://github.com/nicolaigrv/ForceBattle).

**Objective types:**

| Objective Type     | Description                         |
|:-------------------|:------------------------------------|
| ForceItem          | Collect an item                     |
| ForceMob           | Kill a mob                          |
| ForceBiome         | Discover a biome                    |
| ForceAdvancement   | Complete an advancement             |
| ForceHeight        | Reach a certain height              |
| ForceCoords        | Reach a certain coordinate          |
| ForceStructure     | Discover a certain structure        |
| ForceDeath         | Get killed by a certain damage type |

Objective types can be enabled or disabled individually for the players / teams via the plugin's settings menu.

---

## ✨ Features

- Adjustable **battle timer**
- **Chain Mode** - each player has to complete the same chain of objectives
- **Team system**
- Integrated **bossbar** progress display
- Extensive **settings menu** (in-game GUI)
- **Simplified** objective settings for easier gameplay

### 📋 Commands

| Command                          | Description                                           |
|:---------------------------------|:------------------------------------------------------|
| `/backpack`                      | Opens the personal or team backpack (can be disabled) |
| `/displayresults`                | Show the final leaderboard (player, team)             |
| `/exclude <player>`              | Exclude a player from the battle                      |
| `/help`                          | Display a list of available commands                  |
| `/joker <player> <amount>`       | Adjust jokers for a player                            |
| `/language`                      | Open the language selection menu                      |
| `/points <player> <amount>`      | Modify a player's points                              |
| `/randomteams <teamsize> <mode>` | Creates random teams with a specific size             |
| `/recipe <item>`                 | Displays the recipe of an item                        |
| `/reset [player]`                | Reset the battle or a specific player                 |
| `/result <player>`               | Show detailed results for a player                    |
| `/settings`                      | Open the plugin's settings menu                       |
| `/skip <player>`                 | Skip a player's current objective (for admins)        |
| `/team`                          | Manage teams                                          |
| `/timer`                         | Control the timer (start, stop, set duration)         |

---

## 🛠️ Building from Source

To build ForceBattle yourself, make sure you have **Gradle** installed.  
Then clone this repository and run:

```bash
gradle build
```

The compiled `.jar` file will be located under `build/libs` inside the `Loader` module.

---

## 🤝 Contributing

Want to contribute? Awesome! Here's how you can help:

1. **Fork** this repository.
2. **Create a new branch** based on your feature or fix.  
   (Example: `feature/new-feature` or `fix/bug-description`)
3. **Implement your changes**, following `.editorconfig` and standard Java conventions.
4. **Test your changes** carefully to ensure nothing breaks.
5. **Push** to your fork and **open a pull request** with a clear description of what you changed.

---

## 📜 License

This project is licensed under the **GNU GPLv3**.  
See the [LICENSE](https://www.gnu.org/licenses/gpl-3.0) file for details.
