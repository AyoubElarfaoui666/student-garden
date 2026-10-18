# Student Garden for Android

A personal student desk that opens straight into today's work. This is a native Android
application with offline records, private media, and no required account or network service.
The web app remains separate and unchanged. A portable backup is the explicit transfer boundary.

## Identity

Jira blue #0052CC is the anchor. Paper-white #FFFFFF reading surfaces sit on #F6F7F9,
with dark ink #1C1E21 and secondary text #606770. The feature accents retain their jobs:
gold #FFC933 for notes; coral #F56C61 for tasks; calendar blue #3895F5 for planning;
green #20B66A for habits; teal #16B8A6 for focus; cyan #16A6C2 for lesson reviews;
orange #F59B38 for flashcards. Each accent pairs with a darker readable label. Habits
and agenda sessions can choose their own color; agenda sessions also accept #RRGGBB values.
The launcher and app header use an original young sapling: a small curved trunk and
three fresh leaves on the blue app tile. Dark mode uses deep neutral surfaces and readable accent variants.

Source Sans 3 is the interface and reading face. Arabic uses Noto Sans Arabic with
mirrored navigation. Fonts are bundled under SIL OFL and scale with system settings.

## Native structure

Version 0.5.0 continues the existing native layout. Today, Plan, Study, Cards, and Habits
are the five persistent destinations; wide windows use a navigation rail. The top bar
provides Focus and Settings. Study contains notes, courses, and lesson reviews. Cards
opens a deck library, then a nine-card grid with search. Notes and recall use full-screen
editors. Note formatting uses visible toggle states and a wrapped toolbar; native
table editors support draft recovery and confirmed removal of filled cells.
Plan combines tasks with a day-filtered, week-navigable agenda. Settings exposes
backup, language, appearance, device lock, and Trash. Agenda editors show the
selected calendar day as a read-only label and expose only a time picker.

Color distinguishes features while navigation stays blue. Today prioritizes actual
work and review counts; empty agenda copy is omitted. Tasks use checkable rows, notes
use paper surfaces with a gold edge, and habits use independently colored check-ins.
The card front offers a single reveal action. Only the back shows the two-by-two rating
controls and the next long-term interval. No generated image is used as a UI screenshot
or embedded interactive page: these are real Compose controls and persistent records.

Use 48dp touch targets, 16dp page gutters, 24dp section gaps, 8dp control spacing, and
12dp sheet corners. A task is a row; a note is a quiet writing surface; a habit is a weekly
ledger. Avoid applying the same floating card to every type of content. No gradients,
glow, motivational filler, decorative numbers, or automatic entrance animations.

## Motion

Motion responds to an action: a short destination crossfade, a deliberate card flip,
a checked-day transition, and a bottom sheet that follows Android conventions. State
survives rotations and process recreation. Respect the system animator duration setting.
Long lists use keyed lazy layouts. Database reads, media work, and backup parsing stay
off the UI thread. Release builds use R8. Performance claims require measured evidence.

## Native behaviors

Cards support text, images, audio files, and microphone recordings; prompts and answers
stay separate. Again / Hard / Good never advance the separate 48h → 7 days → 1 month →
Complete step. Decks isolate study queues. Lessons use their own 48h → 7 days → 1 month
reviews. Personal habit schedules preserve fixed time zones and existing check-in history.

The focus timer uses a persisted end time and a native completion notification/alarm.
The foreground timer remains visible when needed; no always-on polling or network service.
Media selection uses the system picker. Recording requests microphone access only when
requested, checks revocation, and stops when the editor leaves. A pinned red status
shows Recording, elapsed time toward the three-minute limit, and Stop while recording.
Deletion messages offer Undo for three seconds; Trash retains recoverable records. Backups use the system
file picker and validate paths and sizes before restoring. Existing data is never silently
replaced by an import; previews and explicit confirmation explain a restore.

## Privacy and distribution

No analytics, ads, Firebase, Google Play Services, account keys, or required hosted service.
No personal email/password is included in the source or APK. Private app storage and an
optional device-credential lock protect local records. F-Droid metadata describes only
implemented features. Public source hosting, a release tag, and F-Droid maintainer review
remain publishing steps; a generated APK does not mean acceptance into the repository.
