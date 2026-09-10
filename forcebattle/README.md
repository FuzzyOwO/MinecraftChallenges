# ForceBattle

**ForceBattle** is a multiplayer gamemode where players compete to complete randomized objectives and earn points.  
The player or team with the most points after the timer ends wins the battle!

*(Based on the original ForceBattle gamemode by nicolaigrv which is available [here](https://github.com/nicolaigrv/ForceBattle)).*

---
## ✨ Features

- **Chain Mode**: Force all players to complete the exact same sequence of objectives.
- **Joker Items**: Use Skip items to bypass difficult objectives or Swap items to trade objectives with opponents.
- **Team Backpacks**: Shared inventory storage for team members.
- **Leaderboards & Results**: Detailed end-game results and live leaderboard tracking.

---

## 🎯 Objective Types

| Objective Type       | Description                          |
|:---------------------|:-------------------------------------|
| **ForceItem**        | Collect a specific item              |
| **ForceMob**         | Kill a specific mob                  |
| **ForceBiome**       | Discover a specific biome            |
| **ForceAdvancement** | Complete a specific advancement      |
| **ForceHeight**      | Reach a specific height              |
| **ForceCoords**      | Reach specific coordinates           |
| **ForceStructure**   | Discover a specific structure        |
| **ForceDeath**       | Get killed by a specific damage type |

Objective types can be enabled or disabled individually via the in-game settings menu.

---

## 📋 Commands

| Command                          | Description                                           |
|:---------------------------------|:------------------------------------------------------|
| `/backpack`                      | Opens the personal or team backpack                   |
| `/displayresults`                | Shows the final leaderboard                           |
| `/exclude <player>`              | Excludes a player from the battle                     |
| `/joker <player> <amount>`       | Gives or adjusts jokers for a player                  |
| `/points <player> <amount>`      | Modifies a player's points                            |
| `/randomteams <teamsize> <mode>` | Creates random teams with a specific size             |
| `/recipe <item>`                 | Displays the crafting recipe for an item              |
| `/result <player>`               | Shows detailed objective results for a player         |
| `/skip <player>`                 | Skips a player's current objective (Admin)            |
