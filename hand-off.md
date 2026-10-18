# Hand-off: android-tv branch

## Summary

This branch fixes two TV-specific bugs in the TorrentSearch app:

1. **Missing TV feature declarations** – The app lacked the required `LEANBACK_LAUNCHER`, `TOUCHSCREEN`, and `FAKETOUCH` feature declarations, causing the app to be excluded from TV stores.
2. **Focus-escape bug** – All 8 UI screens used `TopAppBarDefaults.enterAlwaysScrollBehavior()`, which hid the top bar buttons when scrolling. This caused the back button to become focusable and disappear from the viewport, breaking navigation on TV.

Both issues are now resolved.

## Changes Made

### AndroidManifest.xml
- Added `uses-feature` declarations for TV support:
  - `android.software.leanback` (optional)
  - `android.hardware.touchscreen` (optional)
  - `android.hardware.faketouch` (optional)
- Added `LEANBACK_LAUNCHER` category to the intent filter
- Added `android:banner` pointing to `app_banner.png`

### Screen Updates (8 screens)
All screens were converted from `TopAppBarDefaults.enterAlwaysScrollBehavior()` to `TopAppBarDefaults.pinnedScrollBehavior()`:
- `BookmarksScreen.kt`
- `BrowseScreen.kt`
- `SearchScreen.kt`
- `SearchHistoryScreen.kt`
- `SearchProvidersScreen.kt`
- `SettingsScreen.kt`
- `DefaultSortOptionsScreen.kt`
- `TorrentDetailsScreen.kt`

### Additional Files
- Created `app_banner.png` (drawable for TV launch banner)
- Updated `FocusAuditTest.kt` (added focus-audit tests)

## Verification

- **Build**: Successfully built APK on `tvtest` emulator
- **Leanback Launcher**: Verified the app appears in TV store listings with `torrentsearch` entry
- **Focus Escaping**: Reproduced the focus-escape issue (focus disappearing after LEFT with hidden top bar) and confirmed the fix (focus now lands on the visible back button)

## Branch Status
- Branch: `android-tv`
- Base: `origin/main` (upstream commit `100b3f21`)
- Status: Clean, ready for review

## Next Steps
- Merge `android-tv` into `main`
- Ensure TV-specific build variants are enabled in CI
- Consider adding TV-specific test coverage (already added via `FocusAuditTest.kt`)
