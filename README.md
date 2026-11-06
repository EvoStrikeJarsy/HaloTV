# Halo TV — IPTV player for Fire TV Stick 4K

An Apple TV–style IPTV player built with Jetpack Compose for TV and ExoPlayer (Media3).
You bring your own M3U playlist (from your IPTV provider); Halo plays it.

## What's inside

- **tvOS-style interface**: pure-black backdrop, focused tiles lift with a glow and border,
  pill tabs across the top (Live TV, Favorites, Search, Settings), a large dimmed logo of the
  focused channel in the background, Inter typeface.
- **Fast**: playlists are cached on disk so the app opens instantly; streaming parser handles
  very large playlists; logos are decoded at thumbnail size; small start-up buffer and
  MPEG-TS "start on any keyframe" for quick channel changes; release build is R8-optimised.
- **Playback**: HLS (.m3u8) and MPEG-TS (.ts / Xtream "output=ts"), per-channel User-Agent and
  Referer (from `#EXTVLCOPT` or `url|User-Agent=...`), auto-reconnect, decoder fallback.
- Favorites (hold OK on any tile), search, auto-refresh every 24 h.

## Remote controls

| Where | Button | Action |
|---|---|---|
| Browse | OK | Play channel |
| Browse | Hold OK | Add / remove favorite |
| Browse | Back | Return to Live TV tab, then exit |
| Player | Up / Down (or ⏩ / ⏪) | Next / previous channel (hold to scroll, it tunes when you let go) |
| Player | OK / Left / Right / Menu | Show or hide channel info |
| Player | Play/Pause | Pause / resume |
| Player | Back | Return to the channel list |

## 1. Build the APK

### Option A — GitHub (no software to install)
1. Create a new GitHub repository and upload the contents of this folder (keep `.github/`).
2. Open the **Actions** tab. The "Build APK" workflow runs automatically (or press *Run workflow*).
3. When it finishes (about 5 min), download **HaloTV-apk** from the run's *Artifacts* section and unzip it.
   You'll get `app-release.apk`.

### Option B — Android Studio
1. Install Android Studio (Koala or newer) and open this folder.
2. Let Gradle sync, then **Build → Generate App Bundles or APKs → Build APK(s)**,
   or choose the `release` build variant for the fastest version.
3. APK: `app/build/outputs/apk/release/app-release.apk`

## 2. Install on the Fire TV Stick 4K

**Enable developer mode:** Settings → My Fire TV → About → select your device name 7 times.
Then Settings → My Fire TV → Developer options.

### Using the Downloader app (easiest)
1. Install **Downloader** (by AFTVnews) from the Amazon Appstore.
2. Developer options → *Install unknown apps* → turn on for Downloader.
3. Put the APK somewhere with a direct download link (for example a GitHub *Release* asset or Dropbox
   link ending in `?dl=1`) and type that link into Downloader. Choose *Install*.

### Using ADB from a computer
1. Developer options → turn on *ADB debugging*.
2. Find the Stick's IP: Settings → My Fire TV → About → Network.
3. On your computer:
   ```
   adb connect 192.168.1.50:5555
   adb install -r app-release.apk
   ```
   Accept the prompt on the TV the first time.

Halo appears in **Your Apps & Channels**; you can move it to the home row.

## 3. First launch
Select the link field, press OK, type your M3U link, press Done, then *Load playlist*.
Tip: the **Amazon Fire TV** phone app gives you a phone keyboard for typing long links.

Xtream Codes accounts work through their M3U link:
`http://SERVER:PORT/get.php?username=USER&password=PASS&type=m3u_plus&output=ts`

## Customising
- Colours and fonts: `app/src/main/java/com/halotv/app/ui/theme/Theme.kt`
- Tile size and look: `ui/Components.kt` (`CardWidth`, `ChannelCard`)
- Buffering vs. speed trade-off: `PlayerActivity.createPlayer()`
- Banner/icon: `res/drawable-xhdpi/banner.png` (640×360) and `res/mipmap-xxxhdpi/ic_launcher.png`

## Notes
- Builds from GitHub Actions are signed with a fresh debug key each run, so to install a newer
  build over an older one you may need to uninstall the old one first (favorites will reset).
- Fonts: Inter, SIL Open Font License.
