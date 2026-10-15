# Release checklist

Status as of 8 October 2026, version 1.0.0 (versionCode 1).

✅ = done and verified · 🔲 = to do before publishing · ⚠️ = known limitation

## Before uploading to Google Play

- 🔲 **Create the upload keystore** and `keystore.properties` (see README → Release signing), then build a signed bundle with `./gradlew bundleRelease`. The bundle built here is **unsigned**.
- 🔲 **Add a support email** to `PRIVACY.md`, host the policy at a public URL, and add that URL to the Play listing.
- 🔲 **Fill in the Play Data safety form.**
  - No data collected or shared by default.
  - If the assistant ships, declare that the user's question, optional photo and plant/tank details are sent to your server and the AI provider when the user asks.
- 🔲 **Assistant.** Either deploy `/backend` (HTTPS) and set `assistant.properties` → `baseUrl`, or leave it empty to ship without the assistant.
  - Set a spending limit in the Anthropic Console.
- 🔲 **Screenshots** for the README and the Play listing.
- 🔲 **Target SDK.** The app targets API 36; Lint notes API 37 exists. Review Android 17 behaviour changes and test before raising `targetSdk`.

## Needs testing on a real phone

The emulator (Pixel, API 36) can't fully cover these.

- 🔲 **Reboot.** Reminders still fire after a restart (`BOOT_COMPLETED` → reschedule).
- 🔲 **Time zone and clock change.** Reminders move to the new local time and nothing is duplicated.
- 🔲 **Battery savers.** Reminders arrive in Doze and with OEM battery optimisers on (Xiaomi, Oppo, Vivo, Samsung).
- 🔲 **Exact timing.** Reminders arrive within a few minutes of the set time. WorkManager doesn't guarantee to-the-minute delivery.
- 🔲 **Notification actions** (Mark done, Snooze) work from the lock screen.
- 🔲 **Camera capture** on a physical camera (the emulator camera is simulated), plus Photo Picker imports of large photos.
- 🔲 **Backup and restore**, end to end:
  - a zip backup with photos;
  - restore of a real backup through Google Drive / Files, in both Replace and Merge;
  - restoring a backup on a second phone.
- 🔲 **Calendar export.** The `.ics` file imports into Google Calendar with correct repeats.
- 🔲 **TalkBack**, read through every screen: focus order, labels, the chart descriptions.
  - So far checked by code review and the automated Lint and Compose semantics only.
- 🔲 **Assistant against the real backend**, with a real API key:
  - answer quality;
  - photo questions;
  - the 429 path from the real rate limit.
  - So far tested only against a stand-in backend.
- 🔲 **Older Android.** API 26–28 behaviour: notification channels, file picker, CameraX.
- 🔲 **Small screens** (≤ 360 dp wide) and **tablets / landscape**. The UI is phone-portrait first.

## Verified in this build

- ✅ `testDebugUnitTest`: 119 tests pass.
- ✅ `connectedDebugAndroidTest`: 7 tests pass on the API 36 emulator:
  - UI flows: onboarding, add plant, mark task done, log water test, backup screen;
  - the migration pattern;
  - a database backup round-trip.
- ✅ Backend `npm test`: 7 tests pass.
- ✅ Android Lint: 0 errors. The one remaining warning is OldTargetApi, which is deliberate (see above).
- ✅ ktlint (Android Studio style, `.editorconfig`): 0 findings.
- ✅ The release build with R8 and resource shrinking runs on the emulator:
  - onboarding, add plant, Home, task completion and the tabs work;
  - Hilt WorkManager workers succeed;
  - no crashes in logcat;
  - APK is 4.0 MB.
- ✅ Cold start of the release build on the emulator: about 1.4–1.7 s warm-process, 6 s on the very first launch, which includes seeding and compiling.
- ✅ 200% font size: Home, navigation and forms readable. The bottom-bar labels and quick-add grid were adjusted for it.
- ✅ Colour contrast: all 25 colour pairs in light and dark themes are at least 5.2:1 (WCAG AA).
- ✅ Every Room DAO call is `suspend` or `Flow`, so no database work runs on the main thread.

## Known limitations

- ⚠️ **Task titles and catalog text are in English only.**
  - Task titles are stored in English when a task is created.
  - The UI strings are in resources and ready to translate; the catalogs aren't yet.
- ⚠️ **Reminder timing** uses WorkManager, so reminders can arrive a few minutes late under Doze. There are no exact alarms, so no special permission is needed.
- ⚠️ **Backend rate limiting** is in-memory per instance. Use your platform's limiter if you run more than one instance.
- ⚠️ **No Room migrations exist yet** because this is schema v1. The migration test checks the pattern only.
- ⚠️ **The analytics charts** are simple custom bar and line charts. They have text descriptions for TalkBack but are not interactive.
- ⚠️ **No baseline profile** is generated for the app's own code. Startup is acceptable; a Macrobenchmark module could improve it later.
