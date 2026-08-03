# Rimembranze

Rimembranze ("remembrances") is a private, local-first Android app for keeping track of the
recurring obligations and appointments of everyday life: car tax and insurance renewals, gym
memberships, medical/therapy sessions with invoicing, and every payment tied to them. Everything
lives in an on-device database — there is no account, no backend, and no network sync.

## Features

- **Items** — group everything under a named entity (e.g. a car, a gym membership, a doctor),
  each tagged with a type (Vehicles, Gym, Medical, Other).
- **Deadlines** — one-off or recurring (monthly/quarterly/semiannual/yearly) due dates with
  configurable reminders (14/7/1 days before or same-day), last-paid amount tracking, and
  automatic rescheduling of the next occurrence when marked as paid.
- **Appointments** — schedule future appointments/sessions, mark them done, track which are
  invoiced vs. still pending, and generate an invoice from a batch of completed-but-unpaid
  sessions.
- **Payment records** — a running history of payments/visits per item, including optional
  insurance-reimbursement tracking (status: pending/approved/rejected).
- **Stats** — per-item spend this year, total spend, session count, and average session cost.
- **Reminders** — local notifications scheduled with WorkManager, surviving app restarts, with
  tap-to-open deep links back into the relevant item.
- **CSV export** — export an item's full payment/appointment history to a CSV file via the
  system's document picker.
- **Privacy** — the app window is flagged `FLAG_SECURE` (blocks screenshots and hides content in
  the recent-apps switcher); all data stays in the local Room database.

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3) for the entire UI — no XML layouts.
- **Room** for persistence (SQLite), with `Flow`-based reactive queries.
- **WorkManager** for scheduling deadline/appointment reminder notifications.
- **Coroutines / Flow** throughout the data and view-model layers.
- **MVVM**: `AndroidViewModel`s expose `StateFlow<UiState>`, screens collect them with
  `collectAsState()`.

## Project structure

```
app/src/main/java/com/example/rimembranze/
├── data/
│   ├── db/            Room entities, DAOs, AppDatabase, type converters, migrations
│   └── repository/     Thin repository layer over the DAOs
├── notifications/      Schedulers that enqueue WorkManager requests for reminders
├── worker/             CoroutineWorkers that build and post the actual notifications
├── ui/
│   ├── vm/              ViewModels (one per screen/scope)
│   ├── components/     Reusable Compose building blocks (cards, dialogs, chips...)
│   ├── theme/           Material 3 theme, color palette, typography
│   ├── MainScreen.kt        Item list, search, filters, upcoming/expired dashboard
│   └── ItemDetailScreen.kt  Deadlines / appointments / payment history for one item
├── MainActivity.kt      Single activity, handles notification deep links
└── VaultApp.kt          Compose entry point
```

## Requirements

- Android Studio (a recent version compatible with AGP 8.9 / Kotlin 2.0)
- JDK 11
- Android SDK: `minSdk 26`, `targetSdk`/`compileSdk 35`

## Building & running

```bash
./gradlew assembleDebug   # build a debug APK
./gradlew test            # run JVM unit tests
./gradlew connectedCheck  # run instrumented tests on a connected device/emulator
```

Or open the project in Android Studio and run the `app` configuration on a device/emulator
running API 26+.

## Permissions

- `POST_NOTIFICATIONS` — requested once on first launch (Android 13+), used for deadline and
  appointment reminders.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — the app shows an in-app banner suggesting the user
  exempt it from battery optimization, since aggressive OS-level power management can silently
  drop scheduled reminder notifications.

## Known limitations & roadmap

See [`docs/ANALYSIS.md`](docs/ANALYSIS.md) for a full write-up of known issues and proposed
UI/UX improvements that haven't been implemented yet.

## License

No license file is currently included in this repository — all rights reserved by default until
one is added.
