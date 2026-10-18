# Student Garden for Android

A native, offline student workspace: tasks, courses, notes, lesson reviews,
flashcard decks, habits and a focus timer. Built with Kotlin and Jetpack Compose.
The web project is separate; creating or installing this app does not modify it.

## Version 0.6.0

This project continues the existing Apache-2.0 native Student Garden app in a new
workspace. The supplied ZIP was a design handoff, not executable app source. Its
website-generation prompts were treated as reference material. The original native
and web projects and their data have not been modified or automatically imported.

This release adds local reminders for tasks, agenda sessions, daily flashcard practice
and scheduled habits, alongside lessons and focus completion. Settings has category
switches, a daily practice time, permission status, optional precise-alarm access
and a test notification. Reminder taps open the relevant page without losing an
unfinished note. Boot, application updates and clock/time-zone changes restore schedules.

Bold has explicit on/off behavior, heading size no longer replaces bold weight,
and separate static regular/bold font faces support visible Latin and Arabic weights.
Previous agenda colors, selected-day planning, three-second Undo messages, recording
status and native note tables remain available. Database schema 1, backup format 1
and the preview package identity remain compatible.

## Features

- Today’s work, a task list with editing and trash/undo, and planned study sessions
  with individual preset or custom hex colors.
- Full-screen rich notes with bold, italic, underline, headings, lists and tables.
  Table cells contain text, up to 5,000 characters per cell; use up to 30 rows and 6 columns. Filled row/column removal
  asks for confirmation, and table edits participate in Undo and Redo.
- Text, image and audio flashcards grouped into decks, including microphone recording
  with a pinned Recording indicator, elapsed time and Stop control.
- Reveal the answer, then choose Again (<1 min), Hard (<6 min), Good (10 min), or
  advance through 48 hours, 7 days, 1 calendar month and Complete. Short practice
  never resets the longer progression. The library shows nine recent cards until searched.
- Lesson reviews at 48 hours, 7 days and 1 calendar month, measured from each
  actual review. The third review completes the lesson. Local notifications work
  with the app closed after you allow notifications in Settings. Android may delay
  these reminders under battery restrictions; force-stopping the app stops alarms
  until you open it again. No online push service is used.
- Task deadlines and agenda starts send local reminders. Due flashcards have one
  daily reminder; habits remind on their saved weekday schedule unless checked.
  Settings → Reminders controls categories and the daily time (default 9:00 AM).
  Habits use their saved time zone; card practice uses the device time zone.
  Allow notifications and optionally precise reminders; system battery restrictions
  can still delay delivery. Notes and courses have no independent deadline.
- Individually colored habits, weekday schedules, check-in history, streaks and a calendar.
- Custom focus sessions (1–180 minutes), quick presets, pause/resume, persisted timing, a testable completion
  sound and Android notifications. Android may delay background alerts without precise-alarm permission.
- English, Arabic with RTL, French and Spanish; light, dark and system appearance.
- Private SQLite storage, system media/file pickers, merge-only backup restore and an
  optional Android device-credential lock. No required server, account, Internet permission,
  ads, analytics, Firebase or Google Play Services.

## Build

Use Android Studio or JDK 17, Android SDK platform 36 and build-tools 35.0.0.
Set `ANDROID_HOME` (or create an untracked `local.properties` containing `sdk.dir=…`).

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

`app/build/outputs/apk/debug/app-debug.apk` is a test-signed preview for installing
on your own phone. `app/build/outputs/apk/release/app-release-unsigned.apk` is the
optimized unsigned release. F-Droid builds from public source and signs its own
release. The preview uses the separate ID `com.jdinohaila.studentgarden.preview`, so
it can stay installed beside the future F-Droid release. Export a backup from the
preview and import it into the release to transfer your data.

A repeatable container build uses the official F-Droid buildserver image:

```sh
./tools/build-in-fdroid.sh /absolute/path/to/Android/Sdk
```

The wrapper validates the Gradle distribution SHA-256. All dependencies have pinned
versions and come from Google Maven, Maven Central or the Gradle plugin portal.
See `licenses/` and `docs/research.md` for dependency and skill provenance.

## Install a preview

Transfer the debug APK to your Android phone, open it and allow installation from
that file app when Android asks. Or, with Android platform-tools and USB debugging:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Minimum Android version: 8.0 (API 26). No Google account is needed.

## Data and backups

Records and media stay in Android’s private app directory. Removing the app removes
that local data. Use **Settings → Export backup** before reinstalling or changing
signing keys. A backup ZIP contains personal notes and media in readable form; store it
privately. Import validates paths, record types and size limits before merging new IDs.
Existing records win ID conflicts. Trash is retained locally and immediately undoable.

Android backups use `student-garden-android` format version 1. The web app continues to
hold its existing data and uses a different format. There is no automatic web sync or
web-to-Android migration in this release. An Android import does not alter the web app.

## F-Droid publishing

See [the publishing checklist](fdroid/README.md). Metadata and store-copy sources are
included. You must publish this complete source, create a release tag and supply the
actual public repository URL before requesting F-Droid inclusion. The included APK and
metadata do not establish F-Droid acceptance.

## Design

The design contract is in [DESIGN.md](DESIGN.md). Jira blue anchors navigation; notes
use gold, tasks coral, planning blue, habits green, focus teal and reviews cyan/orange.
The original sapling logo and editable vector assets are in `branding/`.
Task rows, note surfaces and the habit ledger have different structures suited to their
work. Native navigation adapts to a rail on wider screens.

## License

Apache-2.0. Bundled fonts are SIL OFL-1.1. See `LICENSE`, `NOTICE` and `licenses/`.

## Current PC

Project folder:
`/home/ayoubel/Documents/ChatGPT/android app`

From a terminal:

```sh
cd "/home/ayoubel/Documents/ChatGPT/android app"
./tools/build-local.sh :app:assembleDebug
"$HOME/Android/Sdk/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
```

`tools/build-local.sh` finds the installed local JDK and SDK. The installation command
needs your phone connected with USB debugging enabled, or a running emulator.

## Limits in this preview

Notes accept up to 50,000 text characters. Editing tables requires version 0.5 or later. Image files are limited to 5 MB, audio files
to 10 MB, microphone recordings to three minutes and imported backup ZIPs to 100 MB.
Export contains active records and their referenced media; restore any wanted items from
Trash before exporting them. App preferences and a running timer are not transferred.
If you remove your Android screen lock, configure a screen lock again before using the
app's device-credential gate.

See [verification details](docs/verification.md) for coverage, screenshots and device-check limits.
