# Draft: GitHub issue for `prajwalch/TorrentSearch`

Paste the section below as a new issue on https://github.com/prajwalch/TorrentSearch/issues

---

**Title**

Feature request: Official Android TV / Google TV build (10-foot, D-pad only)

**Labels:** enhancement, android-tv

**Body**

## Summary

TorrentSearch is excellent on phones, but it is not usable on a TV. I would like
an official Android TV / Google TV build that is operable entirely with a D-pad
remote.

I have a working prototype of exactly this, built on top of the current codebase,
and I'm happy to contribute it. This issue is to discuss the approach before
sending a PR.

## Why this is mostly a packaging change, not a rewrite

Roughly 90% of this codebase is already portable as-is:

- All 46 search providers (`providers/`), Torznab (`torznab/`), the provider
  interfaces (`provider/`), Room + all 8 migrations, DataStore settings, the
  filter/sort helpers and every `ViewModel` are plain Kotlin/JVM with no touch
  or phone-specific dependencies.
- The only Android-coupled things that matter are `NetworkClient` (bound to
  `android.webkit.CookieManager` for the Cloudflare challenge) and
  `ConnectivityManager` — both of which behave identically on Android TV.

A TV build is therefore mostly `androidx.tv.material3` composables over the
existing ViewModels, plus a leanback manifest entry.

## What a TV build needs

These are the requirements I hit, which are easy to get wrong and worth
recording:

**Manifest / packaging**
- A `CATEGORY_LEANBACK_LAUNCHER` activity.
- `android.software.leanback`, `android.hardware.touchscreen` and
  `android.hardware.faketouch` all declared `required="false"` — this is a
  hard Google Play TV quality criterion (TV-MT). Without it the build is filtered
  out of TV entirely.
- A 320x180 px banner in `drawable-xhdpi`, **with the app name baked into the
  image** (criterion TV-BN; banners without legible text get rejected).
- If shipped on Play: AAB is mandatory for TV (TV-G1), and from 1 Aug 2026 both
  32/64-bit ABIs and 16 KB native page alignment are required (TV-G6). Note the
  app already ships two native libs transitively (`libandroidx.graphics.path.so`,
  `libdatastore_shared_counter.so`) — both 16 KB compliant, but worth an explicit
  CI check so it does not regress.

**Input model**
- Every gesture affordance needs a D-pad equivalent. The specific things that are
  currently unreachable by remote: swipe-to-delete on bookmarks, the long-press
  Torznab context menu, the long-press "copy query" on search history, and
  pull-to-refresh in four places.
- Text input needs deliberate wiring on TV. A bare `TextField` (or a
  `Surface(onClick = {})` with no focus target) does **not** attach the on-screen
  keyboard on DPAD_CENTER. Wrapping it in a focusable TV `Surface` whose click
  handler focuses the field is what makes the IME appear. Related: while editing,
  BACK would otherwise exit the screen and lose the query, so it must be trapped
  to close the keyboard first.
- A Compose text field consumes DPAD_UP/DPAD_DOWN for cursor movement. If focus is
  left in the field after BACK, the remote stops navigating the screen entirely.
- If `imeAction = ImeAction.Search`, remember that Compose routes the IME action
  to `KeyboardActions.onSearch`, not `onDone`.
- `ModalBottomSheet` from `androidx.compose.material3` has no D-pad focus
  containment; a `Dialog` does.
- A disabled `androidx.tv.material3.Button` is still reachable with the D-pad, so
  it becomes a dead focus stop that swallows OK.

**Focus indication** (this was the hardest part to get right)
- `ButtonDefaults.scale()` and `ClickableSurfaceDefaults.scale()` both default
  `focusedScale` to **1.0f** — focused buttons and surfaces do not scale at all,
  leaving only a soft glow, which is easy to lose at sofa distance.
- In tv-material 1.1.0 the `border` role (`ButtonDefaults.border(focusedBorder = …)`,
  `ClickableSurfaceDefaults.border(focusedBorder = …)`) did not render reliably in
  my build — focused result rows and buttons showed no ring at all. Drawing the
  ring with a foundation `Modifier.border` driven by `onFocusChanged` worked
  reliably.
- Selection and focus must use **different colours**. `primary` in a dark scheme
  can be a near-white green, which is indistinguishable from a white focus ring.
  Using a filled container for selection and an outline for focus keeps the two
  states orthogonal and composable.
- Focus scale is a graphics-layer transform, so it does not change layout bounds.
  `bringIntoView` will park a focused item flush against the viewport edge with the
  enlarged visual clipped off-screen. Reserve padding around focusable items in
  scrolling containers.
- Geometric focus resolution drops the remote into the middle of a wide chip row
  rather than the currently selected chip; use `Modifier.focusProperties` /
  `FocusRequester` to aim at the selected item.

**Note on Compose for TV versions**
`androidx.tv.foundation`'s `TvLazyColumn` / `TvLazyRow` / `TvLazyVerticalGrid`
were deprecated in 1.0.0-alpha11 and **removed** in alpha12. Standard
`LazyColumn` / `LazyRow` have carried the focus behaviour since Compose Foundation
1.7.0. Any guidance or blog posts referring to the TV lazy layouts are outdated.

## Current feature gaps on TV

- Clipboard and system share are unreliable or absent on TV. Magnet links can still
  be opened by an installed TV torrent client (qBittorrent and Transmission both
  ship TV builds), and `.torrent` files can be saved via SAF where a document
  picker exists.
- The Cloudflare WebView challenge needs a full-screen D-pad-trapped WebView.

## Proposal

A `device` product flavor (`mobile` / `tv`) so the phone build is untouched, with
`src/tv/` holding the TV manifest, resources and UI package. Trade-off: adding
flavors changes the Gradle task names and output paths
(`app:assembleRelease` → `app:assembleMobileRelease`,
`app/build/outputs/apk/release/` → `app/build/outputs/apk/mobile/release/`), which
touches CI and any F-Droid / IzzyOnDroid recipe.

I'm happy to split the work into reviewable PRs — leanback plumbing and CI first,
then the search flow, then the remaining screens — and to drop the product flavor
in favour of a separate module if that fits the project better.

Happy to discuss.

---

## Notes for you (not part of the issue)

- The prototype that backs this issue lives at https://github.com/illnar-io/TorrentSearch_TV
  and the APKs are in the fork's `dist-tv/` output locally.
- Before sending, double-check you are happy that this is an *upstream* issue
  rather than a discussion in the fork — upstream may prefer to keep TV support
  in a separate app, since it changes the project's release layout.