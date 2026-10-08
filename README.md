# BPI Inspections

Android app for building and pest inspections (AS 4349.1 - 2007, AS 4349.3 - 2010, AS 4349.0 - 2007), built for a Samsung Galaxy S26 Ultra.

## Install or update on your phone

1. On the phone, open this repository's **Releases** page (github.com/greg1708-gh/bpi-app/releases).
2. Tap the newest `BPI-Inspections-1.0.N.apk` to download it.
3. Open the download and tap **Install**. The first time, Android asks you to allow installs from your browser or Files app: allow it once.

Each new version installs over the old one and keeps your jobs, clients, photos and comment library.

## How it's built

Every push to `main` runs the **Build app** workflow (GitHub Actions). It builds the APK, signs it with the fixed key in `keystore/`, and publishes it as a release. Keep that key: a differently signed version can't install over the old one.

## Stage 1 (this version)

- Clients & contacts, with call / text / email
- Jobs list with Directions (Google Maps) and Start
- New inspection: purpose, scope (AS 4349.0 for structural/special purpose), client & agent from contacts, date, start/finish, duration, double-booking warning, add to Google Calendar
- Property details (12 fields) & areas, with per-job area reorder
- Area checklists with OK / Minor / Major / Safety / N/I, and not-inspected reasons
- Interior rooms you can add or remove
- Findings with full-size photos saved to the Gallery (Pictures › Inspections › address date), classification, location, comment, QBCC-licensed trades
- Comment library (113 starting comments) you can search, insert, edit, add to and delete
- Jump to: areas, rooms and findings
- Settings: business details, default durations, travel buffer, default area order

## Stage 2 (next)

Report PDF, check-before-sending screen (Outlook/Gmail), T&Cs, standard attachments, Google Drive backup, built-in camera locked to ultra-wide · 3:4 · 12M, photo mark-up.

## Code

Kotlin + Jetpack Compose, Room database (`app/src/main/java/au/bpi/inspect`). The starting comment library is `app/src/main/assets/library.json`.
