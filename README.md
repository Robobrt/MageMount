# MageMount Plugin

Плагин для Minecraft, который позволяет игрокам оседлать любое существо с расширенными функциями, такими как кулдауны, лимиты и временная езда. Разработано для Folia (Minecraft 1.20+,1.21+).

A powerful and highly configurable Minecraft plugin that allows players to ride any mob. Designed for Folia (Minecraft 1.20+,1.21+) servers, it includes advanced features like cooldowns, ride limits, and temporary riding.

---

<details>
<summary><strong>🇷🇺 Документация на Русском</strong></summary>

## ✨ Возможности

- **Езда на любом существе:** Игроки с нужным правом могут оседлать почти любое существо, включая других игроков.
- **Полная настройка:** Каждое сообщение, кулдаун и функция могут быть настроены в `config.yml`.
- **Поддержка RGB и Legacy цветов:** Все сообщения поддерживают современные `&#RRGGBB` и классические `&c` цветовые коды.
- **Временная езда:** Автоматически спешивает игрока через заданное время.
- **Ограничитель поездок:** Ограничивает количество раз, которое игрок может оседлать существо за определенный период.
- **Кулдауны:** Предотвращают спам и моментальное спешивание.
- **Черный список:** Позволяет легко запретить езду на определенных существах, таких как боссы.
- **Команды администратора:** Управляйте количеством поездок игроков "на лету".
- **Поддержка Folia:** Плагин создан с учетом многопоточной архитектуры Folia для оптимальной производительности.

## 🛠️ Команды и Права

| Действие / Команда                         | Право                       | Описание                                           |
| ------------------------------------------ | --------------------------- | -------------------------------------------------- |
| (ПКМ по мобу)                              | `magemount.pets`            | Позволяет оседлать мобов и животных.                |
| (ПКМ по игроку)                            | `magemount.player`          | Позволяет оседлать других игроков.                 |
| `/magemount reload`                        | `magemount.admin.reload`    | Перезагружает конфигурационный файл плагина.        |
| `/magemount uses <игрок> [check]`          | `magemount.admin.uses`      | Проверяет оставшееся количество поездок у игрока.   |
| `/magemount uses <игрок> set <кол-во>`     | `magemount.admin.uses`      | Устанавливает количество поездок для игрока.       |
| `/magemount uses <игрок> add <кол-во>`     | `magemount.admin.uses`      | Добавляет поездки игроку.                          |
| `/magemount uses <игрок> reset`            | `magemount.admin.uses`      | Сбрасывает количество поездок игрока до максимума. |

</details>
<br>
<details>
<summary><strong>en Documentation in English</strong></summary>

## ✨ Features

- **Ride Any Mob:** Players with the correct permission can ride almost any entity, including other players.
- **Full Configuration:** Every message, cooldown, and feature can be customized in the `config.yml`.
- **RGB & Legacy Color Support:** All messages support modern `&#RRGGBB` and classic `&c` color codes.
- **Temporary Riding:** Automatically dismounts a player after a configured amount of time.
- **Ride Limiter:** Limit the number of times a player can mount an entity within a certain period.
- **Cooldowns:** Prevent spam-mounting and instant dismounting.
- **Blacklist:** Easily prevent players from riding specific entities like bosses.
- **Admin Commands:** Manage player ride uses on the fly.
- **Folia Support:** Built with Folia's multi-threaded architecture in mind for optimal performance.

## 🛠️ Commands & Permissions

| Command                                    | Permission                  | Description                                      |
| ------------------------------------------ | --------------------------- | ------------------------------------------------ |
| (Right-click mob)                          | `magemount.pets`            | Allows mounting mobs and animals.                |
| (Right-click player)                       | `magemount.player`          | Allows mounting other players.                   |
| `/magemount reload`                        | `magemount.admin.reload`    | Reloads the plugin's configuration file.         |
| `/magemount uses <player> [check]`         | `magemount.admin.uses`      | Checks the remaining ride uses for a player.     |
| `/magemount uses <player> set <amount>`    | `magemount.admin.uses`      | Sets the ride uses for a player.                 |
| `/magemount uses <player> add <amount>`    | `magemount.admin.uses`      | Adds ride uses to a player.                      |
| `/magemount uses <player> reset`           | `magemount.admin.uses`      | Resets a player's ride uses to the maximum.      |

