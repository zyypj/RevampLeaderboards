# RevampLeaderboards

[![SpigotMC](https://img.shields.io/badge/SpigotMC-✔️-blue.svg)](#)  
[![Java 1.8+](https://img.shields.io/badge/Java-1.8%2B-orange.svg)](#)  
[![PlaceholderAPI](https://img.shields.io/badge/PlaceholderAPI-✔️-blue.svg)](#)

Plugin de **leaderboards** para Spigot/Paper que gera rankings **diário**, **semanal**, **mensal** e **total** a partir de qualquer placeholder do PlaceholderAPI.  

---

## 📋 Sumário

- [✨ Features](#-features)  
- [⚙️ Requisitos](#-requisitos)  
- [🛠️ Configuração](#-configuração)  
  - [boards.yml](#boardsyml)  
  - [config.yml](#configyml)  
- [🎯 Placeholders Disponíveis](#-placeholders-disponíveis)  
- [🔧 Comandos](#-comandos)  

---

## ✨ Features

- Rankings **diário**, **semanal**, **mensal** e **total**  
- **Cache** em memória (Guava) para alta performance  
- Suporte a **MySQL** (via HikariCP)  
- Placeholders padrão: `playername`, `uuid`, `amount`  
- Placeholders **customizados** por jogador  
- Criação/remoção de leaderboards em **runtime**  

---

## ⚙️ Requisitos

- Java 1.8 ou superior  
- Spigot / Paper 1.8+  
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)  
- MySQL ou SQLite (HikariCP)  

---

## 🛠️ Configuração

Após o primeiro start, a pasta `plugins/RevampLeaderboards/` conterá:

- `boards.yml`  
- `config.yml`  

---

### boards.yml

Liste aqui os placeholders (sem `%`) que virarão leaderboards:

```yaml
boards:
  - player_kills
  - player_deaths
  - bw_wins
```

> Para cada entrada, serão criadas quatro tabelas no banco:
> `<sanitized>_daily`, `<sanitized>_weekly`, `<sanitized>_monthly` e `<sanitized>_total`.

---

### config.yml

```yaml
database:
  host: "localhost"
  port: 3306
  database: "database"
  user: "username"
  password: ""

messages:
  nobody: "Ninguém"

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

* **database**:

  * `host`, `port`, `database`, `user`, `password` — conexão ao seu MySQL/SQLite
* **messages.nobody**: texto retornado se não houver posição no ranking
* **custom-placeholders**:

  * **key**: identificador interno (pode ser numérico ou texto)
  * **data**: nome do placeholder usado em `%lb_<data>_<period>_<pos>_<board>%`
  * **placeholder**: string do PAPI (use `{player}` se necessário)
  * **can-be-null**: `true` permite valor vazio; `false` sempre retorna algo
* **scheduler**:

  * **update**:

    * `initial-delay-ticks`: ticks até a primeira coleta
    * `period-ticks`: intervalo em ticks entre coletas
  * **reset** — horários para reset diário, semanal e mensal

---

## 🎯 Placeholders Disponíveis

| Sintaxe                              | Descrição                      |
| ------------------------------------ | ------------------------------ |
| `%lb_position_<period>_<board>%`     | Posição do **próprio jogador** |
| `%lb_<type>_<period>_<pos>_<board>%` | Valor de cada posição no top   |

* `<period>` = `daily` | `weekly` | `monthly` | `total`
* `<board>`  = nome exato em `boards.yml`
* `<type>`   = `playername` | `uuid` | `amount` | `<data>` (custom)
* `<pos>`    = posição no ranking (1–10 ou além)

**Exemplos:**

```text
%lb_position_weekly_player_kills%      → posição semanal do jogador
%lb_playername_total_1_player_kills%   → nome do 1º em kills geral
%lb_amount_monthly_3_bw_wins%          → valor do 3º em wins mensal
%lb_tag_daily_2_player_kills%          → custom “tag” do 2º dia
%lb_guild_total_5_bw_wins%             → custom “guild” da posição 5 geral
```

---

## 🔧 Comandos

| Comando                          | Permissão         | Descrição                                          |
| -------------------------------- | ----------------- | -------------------------------------------------- |
| `/lb reload`                     | `leaderboard.use` | Recarrega `config.yml` e `boards.yml`.             |
| `/lb board add <placeholder>`    | `leaderboard.use` | Adiciona um novo leaderboard em runtime (sem `%`). |
| `/lb board remove <placeholder>` | `leaderboard.use` | Remove um leaderboard existente em runtime.        |
| `/lb board list`                 | `leaderboard.use` | Lista todos os leaderboards registrados.           |
| `/lb sensive resetDatabase`      | `leaderboard.use` | Limpa todo o banco de dados (apenas operadores).   |

> **Uso básico**
>
> * `/lb reload`
> * `/lb board add player_kills`
> * `/lb board remove player_deaths`
> * `/lb board list`
> * `/lb sensive resetDatabase`
