# Icon mapping — custom UI → Solar icon set

**Branch:** `opt/solar-icons` · **Date:** 2026-10-10/11 · **Worker:** round-2 icon worker
**Source:** Solar icon set by 480 Design, CC BY 4.0 — https://github.com/480-Design/Solar-Icon-Set
(via Iconify `@iconify-json/solar` JSON, same canonical vector data)

## Style choice: Linear

One style across the whole custom UI: **Solar Linear** (outline, 1.5px stroke on a
24px grid, `stroke-linecap/linejoin=round`). Why Linear and not Bold/Filled:

- The custom UI is a dark music player; outline icons at 1.5px read cleanly at
  14–30px and stay legible against dynamic accent-gradient backgrounds (full player).
- Every pre-existing icon was a single-glyph control; Linear gives a uniform
  visual weight across nav, transport, sheets, and menus (the old set mixed
  MD-filled glyphs with a few stroke ones).
- `stroke="currentColor"` inherits button/text color exactly like the old
  `fill="currentColor"` did, so active-state tinting (green shuffle/repeat,
  secondary dots) works unchanged.
- Exception: hearts stay **filled** (see table) — the like buttons toggle `fill`
  via JS/CSS (`toggleLike`, `.menu-item-row svg{fill:...}`), so the Solar heart
  is embedded as bare paths with the svg keeping its original fill behavior.
  Shape is still 100% Solar Linear (`heart-linear`).

Technical notes:

- All svgs normalized to `viewBox="0 0 24 24"` (Solar's grid); explicit
  `width`/`height`, `id`, and `class` attributes preserved.
- Original non-`currentColor` colors (e.g. `var(--sp-green)`, `var(--sp-black-deep)`)
  carried over as the stroke color so colored icons keep their color.
- The three id'd play/pause svgs (`miniPlayIcon`, `fpPlayIcon`, `headerPlayIcon`)
  are swapped by `innerHTML` in `app.js` — the hardcoded MD path constants there
  (`pauseSvg`/`playSvg`) were replaced with the Solar `pause-linear`/`play-linear`
  path data, and the svg tags got matching stroke attributes.
- Kotlin `${}` pitfall: Solar path data contains no `$`; verified 0 template
  hazards in all replaced regions.

## Counts

| | Before | After |
|---|---|---|
| Inline `<svg>` in custom-UI files | 101 (83 unique shapes) | 101 (75 unique shapes) |
| Replaced with Solar | — | **99 occurrences / 81 unique** |
| Deliberately left alone | — | **2** (brand logos, see below) |

## Mapping table (old location → Solar)

### app/src/main/assets/custom-ui/index.html (55 replaced)

| # | Location / use | Old glyph | Solar (Linear) | Why |
|---|---|---|---|---|
| 1 | library sort button (`openSortMenu`) | MD sort lines | `sort-linear` | same semantics: sort lines |
| 2 | mini player play/pause (`miniPlayIcon`) | MD pause bars | `pause-linear` | play/pause pair |
| 3 | library search button | MD search | `magnifier-linear` | search |
| 4 | "Downloaded" badge | MD check-circle (green) | `check-circle-linear` | check in circle; green stroke kept |
| 5 | full-player next (`skipTrack(1)`) | MD skip-next | `skip-next-linear` | next track |
| 6 | full-player like (`fpHeartIcon`) | MD heart (green) | `heart-linear` (filled) | like; fill kept for JS toggle |
| 7 | bottom nav Home | MD home (filled) | `home-linear` | home |
| 8 | bottom nav Search | MD search (filled) | `magnifier-linear` | search |
| 9 | bottom nav Your Library | MD library shelves (filled) | `library-linear` | library |
| 10 | folder art ("New Folder") | MD folder | `folder-linear` | folder |
| 11 | library header shuffle | MD shuffle | `shuffle-linear` | shuffle; secondary color kept |
| 12 | library header create (+) | MD plus | `add-linear` | add/create |
| 13 | full-player minimize | MD chevron-down | `alt-arrow-down-linear` | collapse chevron |
| 14 | "What's new" (profile sheet) | MD bell | `bell-linear` | notifications |
| 15 | song menu Share | MD share | `share-linear` | share |
| 16 | playlist header back | MD arrow-back | `arrow-left-linear` | back |
| 17 | song menu Copy link | MD link | `link-linear` | link |
| 18 | verified-artist badge (×6) | MD check | `check-linear` | check mark; light (127 B) |
| 19 | full-player previous (`skipTrack(-1)`) | MD skip-previous | `skip-previous-linear` | previous track |
| 20 | top-result play button | MD play triangle | `play-linear` | play |
| 21 | mini player devices | MD phone | `devices-linear` | device picker |
| 22 | library context menu (⋮) | MD more-vert | `menu-dots-vertical-linear` | more options |
| 23 | song menu "Add to playlist" (×2) | MD playlist-add | `playlist-linear` | list + note |
| 24 | full-player track menu (⋮) | MD more-vert | `menu-dots-vertical-linear` | more options |
| 25 | device card "This phone" | MD smartphone | `smartphone-2-linear` | phone |
| 26 | library view-mode toggle | MD grid | `widget-2-linear` | grid view |
| 27 | create-sheet "Blend" | MD group | `users-group-two-rounded-linear` | people/blend |
| 28 | profile sheet Settings | MD gear | `settings-linear` | settings |
| 29 | search input magnifier | MD search (dark) | `magnifier-linear` | search; dark stroke kept |
| 30 | full-player pause (`fpPlayIcon`) | MD pause bars | `pause-linear` | play/pause pair |
| 31 | full-player devices utility | MD phone | `devices-linear` | device picker |
| 32 | search clear (×) | MD close (stroke) | `close-circle-linear` | clear |
| 33 | Liked-Songs card heart | MD heart (white) | `heart-linear` (filled) | like |
| 34 | Liked-Songs row heart | MD heart (white) | `heart-linear` (filled) | like |
| 35 | song menu like heart | MD heart | `heart-linear` (filled) | like; CSS fill cascades |
| 36 | device card "Living Room TV" | MD tv | `tv-linear` | TV |
| 37 | full-player repeat | MD repeat | `repeat-linear` | repeat |
| 38 | pinned "Liked Songs" badge (×2) | MD pin | `pin-linear` | pin; secondary kept |
| 39 | full-player shuffle | MD shuffle | `shuffle-linear` | shuffle |
| 40 | playlist back button | MD arrow-back | `arrow-left-linear` | back |
| 41 | song menu "View artist" | MD person | `user-linear` | artist |
| 42 | voice-search mic | MD mic (dark) | `microphone-large-linear` | voice input; dark stroke kept |
| 43 | "Listening history" | MD history clock | `history-linear` | history |
| 44 | mini player like (`miniLikeBtn`) | MD heart (green) | `heart-linear` (filled) | like; fill kept for JS toggle |
| 45 | playlist header play (`headerPlayIcon`) | MD play triangle | `play-linear` | play |
| 46 | song menu "Song credits" | MD info | `info-circle-linear` | info |
| 47 | song menu "Add to queue" | MD queue list | `list-linear` | list/queue |

### app.js (path constants, innerHTML swaps)

| Location | Old | Solar | Why |
|---|---|---|---|
| `togglePlayState` / `playSong` / `playPlaylist` / `syncInitialPlayer` `pauseSvg` (×4) | MD pause bars path | `pause-linear` paths | matches the swapped svg tags |
| `togglePlayState` / `syncInitialPlayer` `playSvg` (×2) | MD play triangle path | `play-linear` path | matches the swapped svg tags |

### SpotilolPlayer.kt — injected mini/edge player bar (27 replaced)

| Button (`aria-label` / id) | Old glyph | Solar (Linear) | Why |
|---|---|---|---|
| Download (`spl-download`) | Spotify 16px download | `download-linear` | download |
| Cancel download (`spl-dl-cancel`) | X | `close-circle-linear` | cancel |
| Picture in Picture (`spl-pip`) | MD tv | `pip-linear` | PiP |
| Timer (`spl-timer`) | MD stopwatch | `stopwatch-linear` | timer |
| Now Playing (`spl-nptoggle`) | custom rect+chevron | `square-alt-arrow-right-linear` | open panel chevron |
| Previous (`spl-prev-mini` ×2) | Spotify 16px skip-back | `skip-previous-linear` | previous |
| Play (`spl-play-mini`, `spl-play` ×4 via `splPaintPlayIcon`) | Spotify 16px play | `play-linear` | play |
| Pause (via `splPaintPlayIcon` ×2) | Spotify 16px pause | `pause-linear` | pause |
| Like (`spl-liked`) | Spotify 16px heart | `heart-linear` (filled) | like |
| Next (`spl-next-mini` ×2) | Spotify 16px skip-fwd | `skip-next-linear` | next |
| Queue (`spl-queue`) | Spotify 16px queue | `list-linear` | queue list |
| Volume (`spl-vol-btn` ×2) | Spotify 16px speaker | `volume-small-linear` | volume |
| Shuffle (`spl-shuffle` ×2) | Spotify 16px shuffle | `shuffle-linear` | shuffle |
| Repeat (`spl-repeat` ×2) | Spotify 16px repeat | `repeat-linear` | repeat |
| Repeat-one state (`rDisabled`, rc==='mixed') | Spotify 16px repeat-one | `repeat-one-linear` | repeat one |
| Lyrics (`spl-lyrics`) | Spotify 16px mic | `microphone-linear` | lyrics/voice |
| Smart-shuffle sparkle (`spl-sparkle`) | Spotify 16px sparkle | `magic-stick-linear` | AI/smart shuf
...[truncated 5218 chars]