## Setup

For setup instructions please see the [fabric documentation page](https://docs.fabricmc.net/develop/getting-started/setting-up-a-development-environment) that relates to the IDE that you are using.

## Now Playing (YouTube Music) Integration

This mod starts a small local HTTP server on `http://127.0.0.1:18080` to accept "now playing" updates from your browser and exposes a Minecraft command `/nowplay` to show the current track in chat.

### Features
- Local HTTP endpoint to receive now-playing updates
- Last.fm poller for the user’s current track
- Commands usable both server-side and client-side (works on public servers)
- Optional global-chat prefix via `/nowplay g` (prepends `!`)

### Commands
Server-side (если сервер с модом):
- `/nowplay` — отправляет текущий трек в чат
- `/nowplay g` — отправляет в чат `!<сообщение>` (для глобальных чатов)
- `/nowplay lastfm <username>` — меняет ник Last.fm (для исполнившего игрока локально на сервере; из консоли — глобально)
- `/nowplay lastfm api <key>` — устанавливает Last.fm API ключ (глобально на сервере)

Клиентские (работают везде, даже на публичных серверах без мода):
- `/nowplay` и алиас `/np` — отправляет текущий трек в чат от имени игрока
- `/nowplay g` и `/np g` — отправляет `!<сообщение>`
- `/nowplay lastfm <username>` — меняет ник Last.fm локально у клиента
- `/nowplay lastfm api <key>` — сохраняет API ключ локально у клиента

Примечание: если сервер и клиент оба с модом — доступны обе группы команд.

### Где хранится конфигурация
- Глобальный конфиг: `config/youtube-music-nowplaying.properties`
  - Ключи: `lastfm_username`, `lastfm_api_key`
- Персональный конфиг игрока (на сервере): `config/youtube-music-nowplaying/players/<UUID>.properties`
  - Ключи: `lastfm_username`

### Версии
- Цель: Minecraft `1.21.6`, Fabric API `0.127.0+1.21.6`

- Сервер: положите собранный jar мода и Fabric API в `mods/` сервера, запустите сервер. На старте поднимется локальный HTTP-сервер и Last.fm-поллер.

### Сценарии использования
- Только клиент с модом, сервер без мода: используйте клиентские команды `/nowplay` или `/nowplay g` — сообщение уйдет в общий чат как обычное сообщение игрока.
- Сервер с модом: используйте серверные команды. Эндпоинт `http://127.0.0.1:18080` и опрос Last.fm поднимаются на стороне сервера.

### Userscript (Tampermonkey) for YouTube Music
Install a userscript manager (e.g., Tampermonkey) and add the following script. It pushes the current track whenever it changes on `music.youtube.com`.

```javascript
