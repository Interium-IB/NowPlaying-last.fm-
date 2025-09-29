## 🔗 Integration with Last.fm
A mod for integrating information about the current track from **YouTube Music**, **Spotify**, **Last.fm**, and other services directly into Minecraft chat.  
Supports both client and server modes.
The mod launches a local HTTP server at  
`http://127.0.0.1:18080`, which receives data about the current playback from the browser.  
Available services:
- YouTube
- YouTube Music
- Spotify
- SoundCloud
- Deezer
- Sonos
- BandCamp
---
## ⌨️ Commands
### Server
- `/nowplay lastfm api <key>` — sets the Last.fm API key globally on the server
- - `/nowplay lastfm <username>` — changes the Last.fm nickname  
  - locally for the player  
  - globally (if executed from the console)
### Client
- `/nowplay` or `/np` — sends the current track to chat  
- `/nowplay g` or `/np g` — sends `!<message>`  
- `/nowplay lastfm <username>` — changes the Last.fm nickname locally on the client  
- `/nowplay lastfm api <key>` — saves the API key locally
💡 If both the server and client have the mod installed, both command groups are available.
---
## ⚙️ Configuration
- **Global config**:  
  `config/youtube-music-nowplaying.properties`  
  - `lastfm_username`  
  - `lastfm_api_key`  
- **Personal player config (on the server)**:  
  `config/youtube-music-nowplaying/players/<UUID>.properties`  
  - `lastfm_username`
