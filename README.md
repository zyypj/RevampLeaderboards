# RevampLeaderboards

[![SpigotMC](https://img.shields.io/badge/SpigotMC-✔️-blue.svg)](#)  
[![Java 1.8+](https://img.shields.io/badge/Java-1.8%2B-orange.svg)](#)  
[![PlaceholderAPI](https://img.shields.io/badge/PlaceholderAPI-✔️-blue.svg)](#)

A **leaderboards** plugin for Spigot/Paper that generates **daily**, **weekly**, **monthly** and **total** rankings from any PlaceholderAPI placeholder.

---

## 📋 Table of Contents

- [✨ Features](#features)  
- [⚙️ Requirements](#requirements)  
- [🛠️ Configuration](#configuration)  
  - [boards.yml](#boardsyml)  
  - [config.yml](#configyml)  
  - [messages.yml](#messagesyml)  
- [🎯 Available Placeholders](#available-placeholders)  
- [🔧 Commands](#commands)  

---

## ✨ Features

- **Daily**, **weekly**, **monthly** and **total** leaderboards  
- In-memory **cache** (Guava) for high performance  
- **MySQL** support (via HikariCP)  
- Default placeholders: `playername`, `uuid`, `amount`  
- **Custom** per‑player placeholders  
- Runtime creation/removal of leaderboards  

---

## ⚙️ Requirements

- Java 1.8 or higher  
- Spigot / Paper 1.8+  
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)  
- MySQL or SQLite (HikariCP)  

---

## 🛠️ Configuration

After the first startup, the `plugins/RevampLeaderboards/` folder will contain:

* `boards.yml`  
* `config.yml`  
* `messages.yml`  

### boards.yml

```yaml
boards:
  - player_kills
  - player_deaths
  - bw_wins
````

### config.yml

```yaml
database:
  host: "localhost"
  port: 3306
  database: "database"
  user: "username"
  password: ""

custom-placeholders:
  # %lb_tag_<position>_total_<board>%
  0:
    can-be-null: false
    data: tag
    placeholder: "%leaftags_tag_prefix%"

  # %lb_guild_<position>_total_<board>%
  1:
    can-be-null: true
    data: guild
    placeholder: "%leafguilds_guild_colortag%"

scheduler:
  update:
    initial-delay-ticks: 20
    period-ticks: 1200

  reset:
    daily:
      time: "00:00"

    weekly:
      day: "SUNDAY"
      time: "00:00"

    monthly:
      day: 1
      time: "00:00"
```

### messages.yml

```yaml
messages:
  nobody: "Ninguém"
  
  # remaining time placeholders
  when-days-missing: "{dd} {day-meaning}"
  when-hours-missing: "{hh}:{mm} {hour-meaning}"
  when-minutes-missing: "{mm}:{ss} {minute-meaning}"
  when-seconds-missing: "{mm}:{ss} {second-meaning}"
  meaning-days: "dias"
  meaning-day: "dia"
  meaning-hours: "horas"
  meaning-hour: "hora"
  meaning-minutes: "minutos"
  meaning-minute: "minuto"
  meaning-seconds: "segundos"
  meaning-second: "segundo"
  meaning-never: "Nunca"

  commands:
    reload:
      success: "&aConfigurations and boards reloaded!"
    verify:
      reload: "&aConfigurations reloaded!"
      invalidate-cache: "&aCache invalidated!"
      boards-updated: "&aBoards verified!"
    sensive:
      usage: "&4&lERROR! &cUse: {usage}."
      board-not-found: "&4&lERROR! &cBoard not found: {board}."
      boards-del: "&aAll boards cleared from database."
      board-del: "&aBoard '{board}' deleted from database successfully!"
    board:
      usage: "&4&lERROR! &cUse: {usage}."
      add:
        usage: "&4&lERROR! &cUse: {usage}."
        board-added: "&aBoard '{board}' added successfully!"
      remove:
        usage: "&4&lERROR! &cUse: {usage}."
        board-del: "&eBoard '{board}' removed successfully!"
      list:
        top-message: "&6Registered boards:"
        board-message: " &7- &f{board}"
      test:
        usage: "&4&lERROR! &cUse: {usage}."
        board-not-found: "&4&lERROR! &cBoard not found: {board}."
        board-empty: "&cThe board '{board}' is empty!"
        top-message: "&6Top 10 of board '{board}'"
        player-message: "&e{position}. &f{player-name} &7- &f{value}"
        reset-message: "&fResets in &e{remaing}."
```

---

## 🎯 Available Placeholders

| Syntax                               | Description                                 |
| ------------------------------------ | ------------------------------------------- |
| `%lb_position_<period>_<board>%`     | Position of **the player**                  |
| `%lb_<type>_<period>_<pos>_<board>%` | Value of each position in the top           |
| `%lb_remains_<period>_<board>%`      | Time remaining until the leaderboard resets |

* `<period>` = `daily` | `weekly` | `monthly` | `total`
* `<board>`  = exact name in `boards.yml`
* `<type>`   = `playername` | `uuid` | `amount` | `<data>` (custom)
* `<pos>`    = position in the ranking (1–10 or beyond)

**Examples:**

```text
%lb_position_weekly_player_kills%     → player’s weekly position in kills  
%lb_playername_total_1_player_kills%  → name of the top 1 player in total kills  
%lb_amount_monthly_3_bw_wins%         → value of the 3rd position in monthly wins  
%lb_tag_daily_2_player_kills%         → custom “tag” of the 2nd position in daily kills  
%lb_remains_daily_player_kills%       → time until the next daily reset of kills  
```

---

## 🔧 Commands

| Command                          | Permission           | Description                                                       |
| -------------------------------- | -------------------- | ----------------------------------------------------------------- |
| `/lb reload`                     | `leaderboard.reload` | Reloads `config.yml` and `boards.yml`.                            |
| `/lb board add <placeholder>`    | `leaderboard.board`  | Adds a new leaderboard at runtime (without `%`).                  |
| `/lb board remove <placeholder>` | `leaderboard.board`  | Removes an existing leaderboard at runtime.                       |
| `/lb board list`                 | `leaderboard.board`  | Lists all registered leaderboards.                                |
| `/lb board test <placeholder>`   | `leaderboard.board`  | Shows top 10 of `<board>` and then the time until the next reset. |
| `/lb sensive resetDatabase`      | `OP`                 | Clears the entire database (operators only).                      |
| `/lb verify`                     | `leaderboard.verify` | Verifies all boards and reclassifies them.                        |

> **Basic usage**
>
> * `/lb reload`
> * `/lb board add player_kills`
> * `/lb board remove player_deaths`
> * `/lb board list`
> * `/lb board test bw_wins`
> * `/lb sensive resetDatabase`
