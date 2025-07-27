# RevampLeaderboards

[![SpigotMC](https://img.shields.io/badge/SpigotMC-✔️-blue.svg)](#)  
[![Java 1.8+](https://img.shields.io/badge/Java-1.8%2B-orange.svg)](#)  
[![PlaceholderAPI](https://img.shields.io/badge/PlaceholderAPI-✔️-blue.svg)](#)

A high‑performance Spigot/Paper plugin that generates **daily**, **weekly**, **monthly** and **total** leaderboards from any PlaceholderAPI placeholder — now with **SQLite** support and an optional embedded HTTP server for RESTful access.

---

## 🚀 Installation

1. Drop `RevampLeaderboards.jar` into your server’s `plugins/` folder.  
2. Restart the server to generate the default configuration files in `plugins/RevampLeaderboards/`.  

---

## 🛠️ REST API Usage

All API endpoints live under:

```
http://<host>:<port><base-path>
```

By default:

```
http://localhost:8080/api
```

### 1. List all registered boards

* **Endpoint**: `GET /boards`
* **Description**: Returns a list of all board keys.
* **Example**:

  ```bash
  curl http://localhost:8080/api/boards
  ```
* **Response**:

  ```json
  [
    "player_kills",
    "player_deaths",
    "bw_wins"
  ]
  ```

---

### 2. Retrieve a leaderboard

* **Endpoint**: `GET /boards/{key}`
* **Query Parameters**:

  * `period` (optional): `daily` | `weekly` | `monthly` | `total`
    *Default: `total`*
  * `page` (optional): zero-based page index *(default: 0)*
  * `size` (optional): page size *(default: 10; set to 0 for all entries)*
* **Example**:

  ```bash
  curl "http://localhost:8080/api/boards/player_kills?period=weekly&page=0&size=5"
  ```
* **Response**:

  ```json
  {
    "totalItems": 42,
    "totalPages": 9,
    "currentPage": 0,
    "pageSize": 5,
    "items": [
      { "uuid": "uuid‑1", "playerName": "Alice", "value": 128 },
      { "uuid": "uuid‑2", "playerName": "Bob",   "value": 115 }
      // …
    ]
  }
  ```

---

### 3. Get a player’s position

* **Endpoint**: `GET /boards/{key}/{period}/position/{uuid}`
* **Description**: Returns the 1‑based rank of the specified player on the given board and period.
* **Example**:

  ```bash
  curl http://localhost:8080/api/boards/player_kills/weekly/position/123e4567-e89b-12d3-a456-426614174000
  ```
* **Response**:

  ```text
  5
  ```

---

### 4. List all supported periods

* **Endpoint**: `GET /periods`
* **Description**: Returns the list of valid period identifiers.
* **Example**:

  ```bash
  curl http://localhost:8080/api/periods
  ```
* **Response**:

  ```json
  ["daily","weekly","monthly","total"]
  ```

---

### 5. Fetch top entries for every board in a period

* **Endpoint**: `GET /boards/period/{period}`
* **Query Parameters**:

  * `limit` (optional): max entries per board *(default: all)*
* **Example**:

  ```bash
  curl "http://localhost:8080/api/boards/period/daily?limit=3"
  ```
* **Response**:

  ```json
  {
    "player_kills": [
      { "uuid": "uuid‑1", "playerName": "Alice", "value": 12 },
      { "uuid": "uuid‑2", "playerName": "Bob",   "value": 10 },
      { "uuid": "uuid‑3", "playerName": "Carol", "value": 8 }
    ],
    "bw_wins": [
      // …
    ]
  }
  ```

---

### 6. Get historical snapshots

#### 6.1 Board history

* **Endpoint**: `GET /history/board/{key}/{period}`
* **Query Parameters**:

  * `from` (optional): ISO‑8601 date‑time
  * `to`   (optional): ISO‑8601 date‑time
* **Example**:

  ```bash
  curl "http://localhost:8080/api/history/board/player_kills/weekly?from=2025-07-01T00:00&to=2025-07-27T23:59"
  ```
* **Response**:

  ```json
  [
    {
      "snapshotTime": "2025-07-01T00:00:00",
      "entries": [
        { "uuid":"uuid‑1","playerName":"Alice","value":15 },
        // …
      ]
    },
    // …
  ]
  ```

#### 6.2 Player history

* **Endpoint**: `GET /history/player/{uuid}/{key}/{period}`
* **Query Parameters**: same as board history
* **Example**:

  ```bash
  curl "http://localhost:8080/api/history/player/123e4567-e89b-12d3-a456-426614174000/player_kills/daily"
  ```
* **Response**:

  ```json
  [
    { "snapshotTime": "2025-07-25T00:00:00", "value": 7 },
    { "snapshotTime": "2025-07-26T00:00:00", "value": 9 }
  ]
  ```

---

### 7. List online players

* **Endpoint**: `GET /players`
* **Description**: Returns the UUID and name of each player currently online.
* **Example**:

  ```bash
  curl http://localhost:8080/api/players
  ```
* **Response**:

  ```json
  [
    { "uuid":"uuid‑1","name":"Alice" },
    { "uuid":"uuid‑2","name":"Bob" }
  ]
  ```

---

## 🔧 Commands (in‑game)

| Command                     | Permission           | Description                                                        |
| --------------------------- | -------------------- | ------------------------------------------------------------------ |
| `/lb reload`                | `leaderboard.reload` | Reload all configurations (application, boards, config, messages). |
| `/lb verify`                | `leaderboard.verify` | Verify boards and refresh cache.                                   |
| `/lb board add <key>`       | `leaderboard.board`  | Add a new leaderboard at runtime.                                  |
| `/lb board remove <key>`    | `leaderboard.board`  | Remove an existing leaderboard.                                    |
| `/lb board list`            | `leaderboard.board`  | List all registered leaderboards.                                  |
| `/lb board test <key>`      | `leaderboard.board`  | Show top 10 and next reset time for `<key>`.                       |
| `/lb sensive resetDatabase` | `OP`                 | Wipe the entire database (server operators only).                  |

---

## 💬 PlaceholderAPI Integration

RevampLeaderboards ships with a set of built‑in PlaceholderAPI expansions under the `%lb_…%` namespace, plus support for **custom‑placeholders** defined in your `config.yml`.

### Built‑in placeholders

- **Remaining time until next reset**  
```

%lb\_remains\_<period>%

```
- `<period>`: `daily` | `weekly` | `monthly` | `total`  
- Returns a human‑readable duration until the next leaderboard reset (e.g. “in 02h 15m”).

- **Player’s own rank**  
```

%lb\_position\_<period>\_<boardKey>%

```
- `<boardKey>`: your board identifier (e.g. `player_kills`)  
- Returns the 1‑based position of the requesting player.

- **Any column from a given rank entry**  
```

%lb\_<dataType>*<period>*<position>\_<boardKey>%
```

````
- `<dataType>`:
  - `playerName` – display name  
  - `uuid` – player UUID  
  - `amount` – the raw numeric value (formatted)  
- `<position>`: 1‑based rank index  
- Example:
  ```text
  %lb_amount_weekly_1_player_kills%
  ```
  → value of the top player on the **weekly** kills board.
````

### 🎨 Custom placeholders

If you need more fields—tags, guild colors, titles, etc.—define them under `custom-placeholders` in your **`plugins/RevampLeaderboards/config.yml`**:

```yaml
custom-placeholders:
# slot 0 → %lb_tag_<position>_<period>_<boardKey>%
0:
  can-be-null: false          # never return empty (defaults to "")
  data: tag                   # logical name used in the placeholder
  placeholder: "%leaftags_tag_prefix%"

# slot 1 → %lb_guild_<position>_<period>_<boardKey>%
1:
  can-be-null: true           # allow null/empty results
  data: guild
  placeholder: "%leafguilds_guild_colortag%"
````

After reloading (`/lb reload`), you can use:

```
%lb_tag_1_total_player_kills%
%lb_guild_3_daily_bw_wins%
```

* The numeric index you choose becomes part of the placeholder’s **dataType**.
* `can-be-null: false` forces an empty string (`""`) when no value is found; `true` will actually return `null`/empty.
* The `placeholder` field is any valid PlaceholderAPI expression, evaluated per player in offline or online context.
