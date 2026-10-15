# Validation actually performed

Date: 2026-10-14. This file distinguishes checks that ran from tests that were only authored.

## Completed checks

- Read the empty workspace and inspected installed tooling; there was no existing Android repository or AGENTS.md convention to follow.
- Consulted official Android/Kotlin/Gradle release documentation for the pinned API-35 toolchain. Sources are in `docs/VERSIONS.md`.
- Retrieved the real Gradle 8.11.1 wrapper JAR and shell/batch scripts from Gradle's tagged repository. Independently matched the JAR SHA-256 against Gradle's official release checksum. Configured the official distribution SHA-256.
- Ran `python scripts/check-project.py`: passed XML parsing for all three Android XML files, notification-only manifest permission, disabled automatic backup, no service/listener/manifest receiver, wrapper SHA-256 and ZIP integrity, wrapper main class, distribution pin, package namespace, and absence of unfinished-code markers/Bluetooth/Wear imports.
- Ran `sh -n gradlew`: passed shell syntax check.
- Ran `java -version`: OpenJDK 21.0.12.1 is installed.
- Inspected source for notification guards, same/unique ID allocation, repeated-alert request, immutable tap intents, clear-by-app-tag, explicit generation/reset, Unicode-safe chunking and export redaction/cancellation handling. This was source review, not runtime verification.

## Build attempt and blocker

Attempted these tasks through the included wrapper, with `GRADLE_USER_HOME` under `/tmp` and the environment's proxy supplied as Java HTTP/HTTPS proxy properties:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

The wrapper started and attempted to download `https://services.gradle.org/distributions/gradle-8.11.1-bin.zip`, then failed immediately with:

```text
Exception in thread "main" java.net.SocketException: Operation not permitted
```

A direct curl check also could not connect to the configured proxy. Network-enabled download requests did not complete. No Gradle distribution or Maven dependencies were downloaded. There was no automatic-approval rejection message to report; the observed executable failure was a socket restriction.

`ANDROID_HOME` and `ANDROID_SDK_ROOT` are unset. `gradle`, `sdkmanager`, `adb` and `kotlinc` are not on PATH. A search of available tool directories found no Android SDK or Kotlin compiler. No emulator or connected device was available.

**No Kotlin compilation, Android lint, JUnit execution, instrumented test, installation or UI/device run completed. No APK was produced.** The wrapper launch/source integrity checks above do not establish that the application compiles or installs. Run the README commands in an Android development environment before device testing.

## Authored tests, not executed

19 JVM test methods:

- Exact ASCII preset lengths, code-point lengths, deterministic contents, end markers, segment offsets and invalid preset sizes.
- Maximum chunk bodies including prefixes; transitions through 10/100 sections; preserved source order, punctuation and normalized whitespace.
- Empty/blank text, long words, multiline text, long whitespace runs, Unicode/emoji, surrogate-pair safety, deterministic output and 120 varied input/budget cases.
- Previous/next and empty-reader bounds.
- Default export redaction, full-payload opt-in, valid JSON fields and full state serialization round-trip.

One instrumented test:

- Write real DataStore preferences containing settings, selected position, metadata, an attempt and its saved observation; cancel the store scope, reopen the file through a new store instance and compare the full state.

## Outstanding acceptance work

1. Build and run JVM tests/lint; resolve any compiler/lint issues surfaced by a real Android toolchain.
2. Install and complete `docs/PHONE_VALIDATION.md`, including runtime permission denial/grant, blocked-channel handling, phone content/IDs, reader behavior, recreation/restart, one-shot locked test and export cancellation/error flows.
3. Complete `docs/HARDWARE_TEST_CHECKLIST.md` with a real Realme Watch S2 and its Realme Link installation.
4. Establish separately whether Realme Link accepts this custom app, forwards repeat updates/separate entries, works when locked, and provides a readable measured budget and acceptable retention/navigation.

Watch compatibility remains pending. No S2 text limit, scrolling support, update reliability, delivery latency or custom-app forwarding success is assumed.
