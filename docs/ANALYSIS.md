# Code review & UI/UX analysis

This document records what was found during a full read-through of the codebase: bugs that were
fixed directly, bugs that were left as a backlog (either too invasive for a quick pass or touching
files with in-progress work), and UI/UX ideas worth considering. Nothing in the "proposals"
section below has been implemented — it's a menu to pick from.

## Fixed in this pass

| Issue | Where | Fix |
|---|---|---|
| Launcher icon clipped/cropped inconsistently across launchers | `res/mipmap-*/ic_launcher_foreground.webp`, `res/mipmap-anydpi-v26/ic_launcher*.xml` | The adaptive-icon foreground was a raster image with the full orange circle baked in edge-to-edge, instead of content confined to the 66dp "safe zone" of the 108dp viewport. Every launcher mask (circle/squircle/rounded square) was clipping it differently. Replaced with a vector containing only the white checkmark glyph, centered well inside the safe zone; the background color layer + launcher mask now provide the circular shape. Also deleted the dead default-template `drawable/ic_launcher_background.xml` that was never wired up. |
| Notifications showed the default Android Studio robot icon, not the app logo | `notifications/DeadlineReminderScheduler.kt`, `worker/DeadlineOneShotWorker.kt`, `worker/AppointmentReminderWorker.kt` | All three call `.setSmallIcon(R.drawable.ic_launcher_foreground)`. That resource used to be the unused default-template vector (a generic robot face) left over from project creation — never the app's real checkmark logo. Since the drawable now contains the correct checkmark glyph (see row above), every deadline/appointment notification automatically shows the right icon too, with no extra code change needed. |
| Custom dark theme not connected to `MaterialTheme` | `ui/theme/Theme.kt`, `ui/theme/Color.kt` | Every screen hardcodes its own dark/amber palette, but `MaterialTheme` was still built from the Compose template's purple colors with Material You `dynamicColor` enabled. Any default-styled element not explicitly overridden (dialog chrome, ripples, status bar contrast) could show the wrong purple or a wallpaper-derived color, and the whole app forced a dark look even under `LightColorScheme` on light-mode devices. Centralized the real palette into `Color.kt` and built a single fixed dark `ColorScheme` from it. |
| Notification permission re-requested on every app launch | `MainActivity.onCreate` | Added a `ContextCompat.checkSelfPermission` guard so the system prompt only fires when the permission isn't already granted. |
| Silent CSV export failures (and no success feedback either) | `ui/vm/ItemDetailViewModel.writeCsvToUri`, `ui/ItemDetailScreen.kt` | The write was wrapped in a `catch (_: Exception) {}` with no feedback in either direction. Added a `csvExportResult` `StateFlow` (mirroring the existing recurring-payment snackbar pattern) so the user now sees a success or failure snackbar. |
| Dead/stray code | `data/db/ItemDao.kt` | Removed an unused, unrelated `import android.content.ClipData.Item`. |
| Full data backup/restore | `data/backup/`, `ui/vm/ItemsViewModel.kt`, `ui/MainScreen.kt` | Added a JSON export/import of everything (items, deadlines, records, appointments), reachable from the drawer, additive to the existing per-item CSV export. Import is merge-only and deduplicated by content (type+name for items, item+category+due-date for deadlines, item+title+date[+amount] for records/appointments) — re-importing the same backup twice is a no-op the second time. |
| Side drawer content was cut off / not scrollable | `ui/MainScreen.kt` (`NavigationDrawerContent`) | The category list + backup rows sat in a plain, non-scrolling `Column`; once the backup rows were added at the bottom they could become unreachable on shorter screens. The header stays fixed, everything below it now scrolls (`Modifier.weight(1f).verticalScroll(...)`). |
| Appointment notification doesn't deep-link to the right screen state | `MainActivity.kt`, `VaultApp.kt`, `ui/MainScreen.kt`, `ui/ItemDetailScreen.kt`, `ui/components/AppointmentCard.kt` | `AppointmentReminderWorker` already put an `appointmentId` extra on its `PendingIntent`, but nothing downstream read it. Threaded `initialAppointmentId` through the same chain used for deadlines; `ItemDetailScreen` now auto-selects the "Appuntamenti" tab and scrolls/highlights the matching appointment card. Required flattening the Appuntamenti tab's rendering into proper keyed `LazyColumn` items (it used to be one non-indexed composable) so it could be scrolled to the same way the Scadenze tab already is. |
| Only one Room migration defined, no fallback | `data/db/AppDatabase.kt` | Added `fallbackToDestructiveMigration(dropAllTables = true)` as a safety net, since the full schema history before `MIGRATION_2_3` isn't tracked. Any install still on an unhandled version now gets a clean (empty) database instead of crashing on every launch. Add explicit migrations here as the schema evolves further, rather than relying on this fallback long-term. |
| CSV export escaping was minimal | `ui/vm/ItemDetailViewModel.kt` (`buildCsvContent`) | Previously only commas in the notes field were replaced; titles weren't escaped at all and newlines/quotes would produce malformed rows. Every field is now properly RFC4180-escaped (quoted + internal quotes doubled when it contains a comma, quote, or newline) via `csvEscape`/`csvRow` in `ui/vm/ItemDetailLogic.kt`. |
| No test coverage for stats/recurrence logic | `ui/vm/ItemDetailLogic.kt`, `app/src/test/.../ItemDetailLogicTest.kt` | Extracted `computeItemStats` and `nextRecurrenceDate` out of `ItemDetailViewModel` into plain, Room-free functions and added JVM unit tests covering year-to-date spend, average session cost, recurrence rollover, and CSV escaping. |

## Known issues not fixed (backlog)

- **Color palette duplication.** `BackgroundDark`, `SurfaceDark`, `AccentAmber`, etc. are defined
  once in `ui/components/SharedComponents.kt` but re-declared locally (same hex values, different
  `private val`s) in `MarkAsPaidDialog.kt` and `AddRecordDialog.kt`. Now that the real palette also
  lives in `ui/theme/Color.kt` (see fixes above), there's a good opportunity to make every screen
  read `MaterialTheme.colorScheme` and delete the duplicated constants — a larger, mechanical
  refactor across ~6-8 files.
- **No localization path.** `res/values/strings.xml` only contains `app_name`; every other string
  in the app (all Italian) is hardcoded inline in Kotlin. Fine for a single-language personal app,
  but blocks adding a second language later without a large find-and-replace.
- **`FLAG_SECURE` is unconditional and unexplained.** Screenshots are always blocked and the app
  preview is hidden in the recent-apps switcher, with no in-app explanation or way to disable it.
  Reasonable for a privacy-sensitive app, but worth a one-line settings/about mention so it doesn't
  look like a bug to a new user testing screenshots.
- **Play Store listing icon.** `ic_launcher-playstore.png` (512×512) appears to have the same
  tight, edge-to-edge bleed as the old adaptive-icon foreground. It isn't used by the installed app
  (only by the Play Console listing), so it wasn't touched here (no image-editing tooling available
  in this environment), but it should be regenerated (with proper padding) whenever a store listing
  is prepared.

## UI/UX improvement proposals (not implemented)

- **Finish centralizing the theme.** Once screens read `MaterialTheme.colorScheme` instead of
  hardcoded hex values (see backlog above), theming changes (e.g. an accent color tweak) become a
  one-line change instead of a multi-file find-and-replace.
- **Decide on light mode.** Right now the app is dark-only regardless of the system setting; either
  embrace that explicitly (remove the now-unused light-theme machinery entirely, which this pass
  already simplified) or build a real light variant of the palette.
- **Broaden search.** The search bar on the main list only matches item name/notes. Extending it to
  also match deadline categories and appointment titles would make it useful for "when's my next
  X" lookups, not just "which item is X".
- **Home-level summary.** Stats currently exist only per-item (`StatsStrip` in
  `ItemDetailScreen`). A small dashboard card on the main list (e.g. total upcoming spend across
  all items this month) would surface the same data at the point where it's most actionable.
- **Accessibility pass.** Several purely-decorative icons already use `contentDescription = null`
  correctly, but a few informational ones do too (e.g. the check/warning icons in list headers);
  worth an audit. Urgency is communicated mostly through color (amber/red dots and text) — most
  rows do pair it with text labels already, but a couple of pure color-only accents (e.g. the
  pulsing dot) could use a redundant cue for color-blind users.
- **Bulk actions.** Deleting records/appointments is one-at-a-time with an inline confirm step.
  For someone cleaning up years of history, a multi-select + bulk delete would help.
- **Localize via `strings.xml`.** Moving the hardcoded Italian strings into resources doesn't
  change behavior today but removes the biggest blocker to ever supporting a second language.
- **Google-account-linked sync.** The JSON backup format was chosen specifically so a future sync
  (e.g. Drive-backed) has a stable, structured base to build on without another format migration.
