# WatchDoc Prototype

An Android **phone** application for measuring whether ordinary Android notifications provide a usable document-reading path to a Realme Watch S2 through Realme Link. Package: `com.watchdoc.prototype`. Android 8.0/API 26 or newer.

**Implementation status:** source and Gradle wrapper are supplied. This workspace has Java 21 but no Android SDK, Gradle distribution, emulator or connected phone. The build attempt was blocked while downloading Gradle by the execution environment's socket restriction. No APK was produced and the Kotlin tests/lint have not run. See [VALIDATION.md](VALIDATION.md) for actual checks and remaining validation.

The S2 is not treated as Wear OS. Install this APK on the phone only. The prototype contains no watch application, Bluetooth code, document parser, OCR, cloud service, account or analytics. Realme Link manages the watch connection and forwarding. Custom-app forwarding and all S2 reading behavior remain unverified. The [official S2 support reference](https://www.realme.com/in/support/kw/doc/2190876) is the reference supplied for Realme Link notification synchronization; actual settings and custom-app eligibility must be checked on the installed version.

## Build

Use Android Studio with support for AGP 8.9 (Meerkat or newer), or the command line. Use **JDK 17** as the reproducible baseline. Gradle 8.11.1 also supports running on JDK 21, which is what the provided environment contains. Do not upgrade individual dependencies independently for this prototype.

Pinned toolchain:

| Component | Version |
| --- | --- |
| minSdk | 26 |
| compileSdk / targetSdk | 35 / 35 |
| Android build tools | 35.0.0 (AGP default) |
| Android Gradle Plugin | 8.9.2 |
| Gradle wrapper | 8.11.1 |
| Kotlin / Compose compiler plugin | 2.1.20 / 2.1.20 |
| Compose BOM | 2025.04.01 |
| AndroidX Core / Activity | 1.15.0 / 1.10.1 |
| Lifecycle / DataStore | 2.9.0 / 1.1.4 |
| Coroutines / Kotlin serialization | 1.10.2 / 1.8.1 |

These are pinned stable releases, not a claim to use the latest versions. Compatibility and release references are in [docs/VERSIONS.md](docs/VERSIONS.md).

1. Open this directory in Android Studio. Let it install Android SDK Platform 35, build-tools 35.0.0, command-line tools and platform-tools; accept Google's SDK licenses in the SDK Manager.
2. Select JDK 17 for Gradle. Let Studio create `local.properties` with `sdk.dir` pointing to your SDK; do not commit it.
3. Sync, then build the `app` debug variant.

Command-line alternative, after installing the Android SDK command-line tools and JDK:

```sh
export ANDROID_HOME=/absolute/path/to/android-sdk
export JAVA_HOME=/absolute/path/to/jdk-17
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
sdkmanager --licenses
chmod +x gradlew
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

On Windows use `gradlew.bat` and configure the same SDK/JDK through Studio or Windows environment settings. The first build needs access to Google Maven, Maven Central, the Gradle plugin portal and Gradle distributions. Once installed, the application itself works without internet access. Realme Link may separately need its own setup connectivity.

Expected output **after a successful build**: `app/build/outputs/apk/debug/app-debug.apk`. Unit reports: `app/build/reports/tests/testDebugUnitTest/index.html`. Lint: `app/build/reports/lint-results-debug.html`. A failed wrapper download is not a test run.

## Install on the phone

With USB debugging enabled and the phone authorizing your computer:

```sh
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.watchdoc.prototype/.MainActivity
```

Alternatively transfer the debug APK to the phone, open it in the file manager, and allow installation from that source when Android asks. Existing installation updates must use the same signing key. Uninstalling or clearing app data removes local history; export it first. Debug APKs use the development machine's standard debug signing key and are for testing.

## Use the four tabs

**Setup:** check notification permission, overall app notification enablement and channel importance; request `POST_NOTIFICATIONS` on Android 13+, open app/channel settings, or open Android notification-access settings for **Realme Link**. WatchDoc never requests notification-listener access. Follow the pairing/synchronization checklist and send one short test. There is no watch-connected indicator: check the watch connection in Realme Link.

For a locked-phone test, configure a secure PIN/password lock that locks immediately on the power button. Tap **Arm one test for the next screen-off**, then press power. A dynamically registered receiver consumes the arm once and asks for one short test only if `KeyguardManager` reports locked. No timer, background service or persistent arm exists. If Android reports unlocked, no notification is sent; retry with immediate-lock settings. Activity destruction/process death cancels arming. This gives a manually controlled way to test locked posting; OEM lock timing and background behavior still need measurement. Wait for the watch, then unlock the phone to inspect the record.

**Notification Lab:** edit title/body or generate exact 100/250/500/1,000 ASCII-character bodies. The title is separate. Each body has numbered 25-character segments and an exact `|END-NNNN|` suffix. The final numbered segment may be partial. Set delivery mode and body format, then Send. Clear removes only notifications tagged by this prototype and preserves history. Android limits each NotificationCompat text field to 5,120 UTF-16 units; the Lab explicitly refuses larger fields instead of recording silently truncated payloads. Reader chunks always stay below that ceiling.

**Text Reader:** starts with an original Computer Networks sample. Edit/paste text, select a 50–1,000 body budget (default 200), and tap **Regenerate sections**. This applies text/budget and explicitly resets position; editing alone does not alter existing chunks. Use Previous, Next or Send Current. Automatic send after phone navigation is off by default. It sends only when an explicit navigation action changes the section. Position, applied text, editor draft, options and settings persist. App startup, rotation, restoration, permission changes and regeneration never send.

**Test Results:** edit session metadata and save manual observations for each attempt. Observation drafts survive recreation while their dialog is open, but you must tap Save observations to commit them. Export uses Android's Create Document picker; cancellation keeps local state, and failures are displayed. The default JSON omits notification titles/bodies; opt in for full payloads. Metadata, notes, IDs, settings, hashes and lengths are always exported. Notes can contain sensitive data, so review them too. A document provider may create a partial file on export failure; choose a new file and retry.

## Notification experiments

| Mode | Android behavior requested | What the tester must establish |
| --- | --- | --- |
| Same ID | Reuse tag `watchdoc_prototype`, ID `4242`; replace title/body; `setOnlyAlertOnce(false)` requests repeated alerts | Whether Realme Link forwards updates and the S2 changes an existing entry, adds another entry or shows nothing |
| Separate IDs | Persistently allocate IDs from `10000`, reserving each before posting | Whether distinct entries arrive and remain usable across multiple sections |
| Standard | Full body in `contentText` | What Realme Link extracts and the S2 displays |
| BigTextStyle | Full body in both `contentText` and `bigText` | Whether this changes forwarding, length, scrolling or layout on the watch |

Mode and style are shared across Setup, Lab and Reader. Both modes use the dedicated **WatchDoc Reading Tests** channel at default importance. Alerts are ultimately subject to channel settings, Android/OEM rate limiting, Do Not Disturb and Realme Link. Notifications are dismissible, do not deliberately group, have no ongoing flag and use immutable tap intents that open their source tab. Android may still automatically group or limit many active notifications. Clear between controlled runs; there is no flood or repeat loop.

Posting success means `notify()` returned locally. It is **not** a receipt from Realme Link or the watch. Every attempt has a UUID, timestamp, Android ID, source, mode/style, body length, SHA-256 body hash and observed Android lock state. Same-ID records explicitly say an update was requested. The attempt/ID is saved before posting; if the process dies between posting and saving the result, the record deliberately reports that completion is unknown and does not resend. A storage error before reservation prevents the send.

## Character budgets and preservation

`TextChunker` is pure Kotlin/JVM logic. A character means a **Unicode code point**, not a UTF-16 unit, byte or visual grapheme. Emoji sequences and combining characters can contain multiple code points. Surrogate pairs are never split; whole grapheme clusters are not guaranteed.

CRLF and lone CR normalize to LF. Other punctuation and whitespace remain in order: concatenating the `text` of all sections reconstructs the normalized document exactly. Empty/whitespace-only documents produce no sections. Boundaries prefer a paragraph, sentence, then whitespace in the latter half of the available span; long unbroken content uses a code-point-safe hard split. A monotonically increasing prefix digit calculation reserves space for `[section/total] `, including transitions through 10, 100, etc. Every final notification body is at most the selected budget; this is not an S2 text-limit assumption.

## Local storage, privacy and architecture

Only `POST_NOTIFICATIONS` is requested. There are no broad storage, contacts, location, Bluetooth, internet or listener permissions. The app has no foreground service, background timer, battery-exemption request, cloud connection or telemetry. Ordinary notification text is deliberately public for testing and can appear on the phone lock screen and watch; use non-sensitive input. Android lock-screen privacy settings may affect what is shown and forwarded. Automatic backup is disabled. Uninstall/clear-data removes private DataStore content; exported reports and already forwarded watch entries are outside that storage.

One application module uses Compose Material 3, a manually injected ViewModel, coroutines/StateFlow, NotificationCompat and Preferences DataStore. A serialized ViewModel action queue orders settings edits, manual sends, ID reservation and result writes. JSON-backed `PrototypeRepository` separates persistence from UI; unreadable state is reported instead of silently replaced. History is retained without automatic pruning; very large sessions/documents can make this lightweight prototype slower. Export and archive measured sessions before starting huge experiments.

Main packages: `ui/setup`, `ui/lab`, `ui/reader`, `ui/results`, `notification`, `pagination`, `data`, `model`. After hardware validation, a document-processing interface can supply extracted PDF/DOCX/PPTX text to the existing reader without changing the notification publisher. Parsing is intentionally excluded now.

Follow [docs/HARDWARE_TEST_CHECKLIST.md](docs/HARDWARE_TEST_CHECKLIST.md) for physical testing and [docs/PHONE_VALIDATION.md](docs/PHONE_VALIDATION.md) for Android acceptance tests. An emulator can check Android behavior but cannot prove Realme Link forwarding to an S2.
