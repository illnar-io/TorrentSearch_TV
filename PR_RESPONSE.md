Hi Prajwal,

Thanks for the thorough review — I appreciate the time you spent going through the diffs. Apologies for the missing screen recording; I'll attach one in a follow-up comment showing the TV navigation flow (D-pad through Home → Search → Browse → Details → Back stack).

Below is my plan to address each point:

---

### 1. Removable comments
**gradle/libs.versions.toml**, **app/build.gradle.kts**, **app/src/tv/res/values/strings.xml** — will strip the inline rationale comments. They were meant as context for reviewers; happy to remove.

---

### 2. README restructure
Move the Leanback section under "Building from source" with two sub-sections: **Mobile** and **TV**. The `adb connect/install` snippet will live under the TV subsection. Will also add a note that `leanback` banner is validated in CI.

---

### 3. Navigation destinations — single source of truth
Agreed — the `Home | Search | Browse | …` sealed objects duplicated in `TorrentSearchTvApp.kt` should be moved to a shared `navigation` package (e.g. `com.prajwalch.torrentsearch.navigation.Destinations`) and re-exported by both the mobile and TV graphs. I'll extract:
```kotlin
// shared/navigation/Destinations.kt
sealed interface Destination {
    @Serializable object Home : Destination
    @Serializable data class Search(...) : Destination
    // …
}
```
Then update both `MobileNavHost` and `TvNavHost` to import from there.

---

### 4. Enum / model extension functions → main package
The `labelRes()`, `displayName()`, `headerLabel()`, `displayText()` helpers currently copied in TV screens will be moved to extension files in the main package (`ui/common/extensions.kt` or alongside the model classes). TV and mobile will both consume them.

---

### 5. Conditional composition vs. early-return
`TvHomeScreen.kt` — `EnableProvidersPrompt` will be wrapped in `AnimatedVisibility(visible = …)` instead of `if (!visible) return`.

---

### 6. `TvCommon.kt` — `Category.displayName()` and `TorrentMetaText`
- `Category.displayName()` → expose from main package (already exists in `ui/categoryStringResource`? if not, add).
- `TorrentMetaText` — replace the `if (value == null) return` with conditional slots:
  ```kotlin
  torrent.size?.let { TorrentMetaText(it) }
  torrent.seeders?.let { TorrentMetaText("↑ $it") }
  // …
  ```

---

### 7. `TvScreenshotCapture` test
This is a **PR-time visual regression helper** — it renders every bottom-sheet / dialog state at 1920×1080 and writes a PNG to `build/reports/tv-screenshots/` so reviewers can diff UI changes without an emulator. It's not a unit test; it's excluded from normal CI by running only on `tvScreenshotCapture` Gradle task. I can either:
- Keep it as a manual dev tool (documented in `CONTRIBUTING.md`), or
- Move it to a `tools/` directory outside `androidTestTv`.
Let me know your preference.

---

### 8. CI build gates (`tv_build.yml`)
Each step enforces a **Google TV / Android TV quality requirement** that would otherwise only be caught at Play Console submission time:

| Gate | What it checks | Why it matters |
|------|----------------|----------------|
| Leanback manifest | `android.software.leanback`, `CATEGORY_LEANBACK_LAUNCHER`, banner drawable | Required for TV Play listing |
| 16 KB page alignment | ELF `PT_LOAD` segment alignment | Mandatory for Android 14+ (ARM64) |
| Both ABIs (arm64-v8a + armeabi-v7a) | `lib/` entries in APK | TV devices ship both; missing one = "not compatible" |
| Unsigned release APK size | < 100 MB (configurable) | Prevents accidental bloat |

They're **necessary** because the TV flavour is a separate distribution track; catching these at PR time avoids rejections and re-releases. If you'd rather make them warnings instead of hard failures, I can flip `run:` to `continue-on-error: true` for now.

---

### Next steps
1. I'll push a cleanup commit with the comment removals, README move, and shared destinations.
2. Follow-up commits for the extension-function extractions and conditional composition fixes.
3. Attach the screen recording.

Let me know if you'd prefer smaller stacked PRs for each category or a single "cleanup" PR on top of the current branch.

Thanks again —
Illnar