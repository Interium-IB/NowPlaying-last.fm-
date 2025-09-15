# Fabric Example Mod

## Setup

For setup instructions please see the [fabric documentation page](https://docs.fabricmc.net/develop/getting-started/setting-up-a-development-environment) that relates to the IDE that you are using.

## Now Playing (YouTube Music) Integration

This mod starts a small local HTTP server on `http://127.0.0.1:18080` to accept "now playing" updates from your browser and exposes a Minecraft command `/nowplay` to show the current track in chat.

### Endpoints
- `POST /nowplaying` with JSON body:
  - `{ "title": "Song Title", "artist": "Artist Name", "source": "YouTube Music" }`
- `POST /clear` clears the current state
- `GET /nowplaying` returns the current state as JSON

### Quick test with PowerShell
```powershell
curl -Method POST -Uri http://127.0.0.1:18080/nowplaying -ContentType 'application/json' -Body '{"title":"Test Song","artist":"Test Artist","source":"YouTube Music"}'
```
Then in Minecraft, run:
```
/nowplay
```

### Last.fm integration
- Set your Last.fm API key via env var or gradle properties.
  - Env var example (PowerShell):
  ```powershell
  $env:LASTFM_API_KEY="YOUR_KEY"; ./gradlew runClient
  ```
  - Or set in `gradle.properties`:
  ```
  lastfm_api_key=YOUR_KEY
  lastfm_username=upsetsummer
  ```
  Then pass as JVM system properties when launching the client or server, for example in your IDE run configuration:
  ```
  -Dlastfm_api_key=%lastfm_api_key% -Dlastfm_username=%lastfm_username%
  ```
  If using `./gradlew runClient`, you can set `ORG_GRADLE_PROJECT_lastfm_api_key` and `ORG_GRADLE_PROJECT_lastfm_username` environment variables and map them into JVM args in your IDE or run config.

### Userscript (Tampermonkey) for YouTube Music
Install a userscript manager (e.g., Tampermonkey) and add the following script. It pushes the current track whenever it changes on `music.youtube.com`.

```javascript
// ==UserScript==
// @name         YT Music → Minecraft Now Playing (Fabric)
// @namespace    yt-music-nowplaying-fabric
// @version      0.1.0
// @description  Send YouTube Music now playing to localhost for Fabric mod
// @match        https://music.youtube.com/*
// @grant        none
// @run-at       document-idle
// ==/UserScript==
(function() {
  'use strict';

  const ENDPOINT = 'http://127.0.0.1:18080/nowplaying';

  function getText(selector) {
    const el = document.querySelector(selector);
    return el ? el.textContent.trim() : '';
  }

  function readTrack() {
    // Common selectors for YouTube Music player bar
    const title = getText('ytmusic-player-bar .title');
    let artist = getText('ytmusic-player-bar .byline a, ytmusic-player-bar .byline');
    if (!artist) {
      // Fallback: page title like "Song • Artist - YouTube Music"
      const t = document.title.replace(/ - YouTube Music$/, '');
      const parts = t.split(' • ');
      if (parts.length >= 2) {
        return { title: parts[0], artist: parts[1] };
      }
      return { title: t, artist: '' };
    }
    return { title, artist };
  }

  let last = { title: '', artist: '' };

  async function pushIfChanged() {
    const { title, artist } = readTrack();
    if (!title && !artist) return;
    if (title === last.title && artist === last.artist) return;
    last = { title, artist };
    try {
      await fetch(ENDPOINT, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title, artist, source: 'YouTube Music' })
      });
      // success
    } catch (e) {
      // ignore; server may be down
    }
  }

  // Observe DOM changes near the player bar
  const bar = document.querySelector('ytmusic-player-bar') || document.body;
  const obs = new MutationObserver(() => pushIfChanged());
  obs.observe(bar, { childList: true, subtree: true, characterData: true });

  // Also poll as a fallback
  setInterval(pushIfChanged, 2000);
})();
```

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
