# MinecraftChallenges

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

---
Supported Minecraft version: 26.3

---

## 🎮 Gamemodes & Modules

MinecraftChallenges is a modular plugin providing multiple unique gamemodes and challenge mechanics.

| Gamemode        | Description                                                                                                             | Documentation                       |
|:----------------|:------------------------------------------------------------------------------------------------------------------------|:------------------------------------|
| **ForceBattle** | Race against opponents to complete randomized objectives (items, mobs, biomes, coords, etc.) and earn the most points.  | [Read Guide](forcebattle/README.md) |
| **DeathSwap**   | Players periodically swap locations—build traps and use fall damage or lava to eliminate opponents.                     | [Read Guide](deathswap/README.md)   |
| **ItemRain**    | Random items periodically fall from the sky or drop directly into your inventory.                                       | [Read Guide](itemrain/README.md)    |
| **Randomizer**  | Randomize block drops, mob drops, and chest loot tables for a chaotic survival experience.                              | [Read Guide](randomizer/README.md)  |
| **AllItems**    | Work individually or as a team to collect every single item, biome, mob, structure, and advancement. *(In development)* | [Read Guide](allitems/README.md)    |

---

## ⚙️ Generic Settings

Access global plugin settings anytime in-game using `/settings`:

- **No Hunger**: Disables hunger depletion for all players.
- **No Damage**: Prevents players from taking environmental or combat damage.
- **Mobile Crafting**: Grants players a portable crafting table menu.
- **Show Scoreboard**: Toggles the sidebar scoreboard display.
- **Timer**: Set the time of the challenge.
- **Teams**: Create and manage teams with shared objectives and team chat.

---

## 📋 Core Commands

| Command           | Description                                                    |
|:------------------|:---------------------------------------------------------------|
| `/start`          | Starts the selected challenge after a 5 second countdown       |
| `/reset [player]` | Resets the game or a specific player                           |
| `/timer`          | Controls the global timer (start, pause, resume, set duration) |
| `/settings`       | Opens the global settings GUI                                  |
| `/team`           | Create, join, or leave teams                                   |
| `/chat`           | Toggles chat mode (global / local)                             |
| `/teamchat`       | Sends a message exclusively to your team                       |
| `/spectate`       | Watch the next battle instead of playing in it (lobby only)    |

---

## 👀 Spectators

Spectators watch a running battle without influencing it:

- **Becoming one**: type `/spectate` in the lobby (again to play). Players who join a running battle spectate automatically, and players knocked out of a DeathSwap become spectators too.
- **Watching**: spectators fly in spectator mode and can right-click a player from up to 10 blocks away to see their armor, inventory, health, position and challenge progress, open their backpack, or view through their eyes.
- **No influence**: spectators get no objectives or items, can't be swapped with, and nothing in their views can be moved.
- **Chat**: during the battle a spectator's chat only reaches other spectators, and `/msg`, `/tell` and `/w` only work towards other spectators.
- **After `/reset`**: late joiners and knocked-out players play again; players who chose `/spectate` keep spectating until they type it again.

---

## 🛠️ Building from Source

To build MinecraftChallenges yourself, make sure you have **Gradle** installed.  
Clone this repository and run:

```bash
gradle build
```

The compiled `.jar` file will be located under `build/libs` inside the `loader` module.

---

## 🤝 Contributing

Want to contribute? Here is how you can help:

1. **Fork** this repository.
2. **Create a new branch** based on your feature or fix.  
   (Example: `feature/new-feature` or `fix/bug-description`)
3. **Implement your changes**, following `.editorconfig` and standard Java conventions.
4. **Test your changes** carefully.
5. **Push** to your fork and **open a pull request**.

---

## 🏝️ Spawn Credits

The lobby spawns where players wait before a game starts were built by:

| Spawn          | Builder          |
|:---------------|:-----------------|
| `classic`      | q8h              |
| `balloninsel`  | IchmagYorkleffs  |
| `inselmithaus` | IchmagYorkleffs  |

---

## 📜 License

This project is licensed under the **GNU GPLv3**.  
See the [LICENSE](https://www.gnu.org/licenses/gpl-3.0) file for details.

---

## ⚠️ Disclaimer

MinecraftChallenges is an independent open-source project and is **NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**  
Minecraft is a registered trademark of Mojang Synergies AB / Microsoft Corporation.

