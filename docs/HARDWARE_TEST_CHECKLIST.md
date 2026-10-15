# Realme Watch S2 hardware test

Status before physical testing: **custom-app forwarding unknown; S2 reading suitability unknown**. A phone notification, emulator result, successful `notify()` call, Bluetooth pairing or the Realme manual alone cannot confirm this app's forwarding.

Use non-sensitive text. Keep the phone with the tester throughout. Have the real S2, its Android phone, Realme Link, a built debug APK, and optionally an external stopwatch/camera. Do not install the APK on the S2.

## Record the session

In Results, enter phone model, Android version, Realme Link version and S2 firmware. Use notes to record date, notification privacy settings, DND, power/background settings, watch notification retention and any setting changes. Keep versions/settings fixed within one comparison run. Export before changing the session metadata for a different device or firmware.

For every attempt, identify its timestamp, source, Android notification ID, title and body. A successful phone status is **Posted on phone — watch delivery unverified.** In Results → Record/edit, enter:

- Seen on watch: Yes / No / Not checked.
- Visible final numbered segment or 1-based body character position (including reader prefix). If exact position is hard to measure, copy the last visible words and mark the estimate.
- End marker visible: Yes / No / Unknown.
- Scroll behavior: Available / Unavailable / Unknown. Distinguish vertical scrolling, expanded text and only a preview.
- Title visible: Yes / No / Unknown; note title changes/truncation.
- Updated existing entry, created a new entry, no entry or unknown.
- Duplicate notifications: Yes / No / Unknown.
- Approximate delay in seconds; blank means unknown.
- Phone locked during test, separately from Android's automatic lock-state snapshot.
- Notes on readability, disappearance, persistence after dismissal, order and settings.

Do not treat “Not checked” as “No.” Choose a consistent observation window, for example 30 seconds, and record it; that window is a test convention rather than a guaranteed delivery deadline. Space manual sends so you can correlate them and avoid Android/OEM alert throttling. If you change the observation window or pause duration, note it.

## Procedure

1. **Install on the Android phone.** Follow README build/install steps. Launch WatchDoc Prototype. Allow notifications on Android 13+. Verify Setup shows enabled application notifications and the **WatchDoc Reading Tests** channel. Send a setup test and first inspect the **phone** notification shade, title/body, icon and tap destination. Record any permission/channel block instead of treating it as a watch failure.

2. **Pair the S2 through Realme Link.** Open Realme Link and follow its official pairing flow. Verify the connection there. The prototype cannot report connection status. Keep the phone and watch nearby.

3. **Enable Realme Link access and synchronization.** In Realme Link's S2 settings, enable phone/message/app notification synchronization. Follow its relevant Android access prompts. Setup's notification-access button opens Android Settings; enable Realme Link's listener access if its workflow requests it. WatchDoc should not appear as a notification-listener service. Note DND and lock-screen privacy settings; adjust manually if needed.

4. **Select WatchDoc Prototype, if available.** Find the per-app forwarding list in Realme Link and select it. If missing, send a setup test, reopen Realme Link and check again. Record whether the app is present and selectable. If it cannot be selected or forwarded, record that limitation. Do not infer support from another app's notifications.

5. **Send exactly 100 ASCII body characters.** In Lab choose 100, Standard text and separate-notification mode. Use a descriptive title such as `S2 100 Standard A`. Check the visible body count is 100; the title is separate. Tap Send once. Inspect the phone and S2, including its stored notification list, within the chosen observation window.

6. **Record the watch result.** Edit the corresponding Results record and save all applicable observations. Look for `|END-0100|`. If absent, record the last visible segment/words/position and whether scrolling reveals more. If no message appears, record No plus the observation window, Realme Link selection state and phone posting status. Keep evidence separate from hypotheses.

7. **Repeat at 250, 500 and 1,000.** Change the title to identify each size and choose its preset. Send each manually and record it before the next send. Look for `|END-0250|`, `|END-0500|` and `|END-1000|`. Clear prototype phone notifications between controlled runs if needed; check separately whether watch entries remain. Never assume the largest received preview equals a guaranteed text limit.

8. **Compare Standard and BigTextStyle.** Repeat all four sizes with Android BigTextStyle and matching conditions/titles. Check Android's expanded view, then the S2 independently. Both Android fields receive the full body; whether Realme Link reads one, both or a truncated representation is unknown. Record duplicate content or altered formatting.

9. **Request at least three same-ID updates.** Clear prototype phone notifications. Choose same-ID mode. Manually send three distinct title/body combinations with labels `Update A`, `Update B`, `Update C`, each spaced and observed separately. You can append A/B/C to a short editable body; preset exact-length measurement is a separate test. All three records should have ID 4242 and update requested = Yes. Record whether the S2 replaces text, alerts again, adds entries, duplicates, changes order or ignores updates. Repeated alerts are requested, but OS/channel rules may override them.

10. **Send at least three separate notifications.** Choose separate mode. Send `Separate A`, `Separate B`, `Separate C` manually with distinct short bodies. Results should show three distinct IDs at or above 10000. Check order, duplicates, usable history depth and how long entries remain. Android may auto-group notifications; do not attribute that to a deliberately configured prototype group.

11. **Repeat a short test with the phone locked.** Configure a secure PIN/password with immediate power-button locking. In Setup tap Arm one test for the next screen-off and immediately press power. Observe the watch without unlocking. The application attempts one short notification only if Android reports locked. Then unlock and inspect Results: require a posted record with `phoneLockedAtAttempt = true` before calling this a locked-posting trial. If the app says no locked test was sent, adjust immediate-lock settings and retry; screen-off alone is not proof of locking. If OEM lifecycle rules cancel the arm or stop execution, record that too. Repeat in the other mode if needed. No timer or foreground service is used. Simply sending while unlocked then locking afterward tests retention while locked, which is a different observation.

12. **Try several reader sections.** In Reader load Computer Networks, leave the initial 200 budget and regenerate. Send Current, then Next + Send Current at least twice; use Previous and send the earlier section. Check body prefixes and title numbers. Measure readability, scrolling, title loss, update vs new-entry behavior and whether phone navigation is acceptable. Try smaller/larger budgets based on measured evidence. Optionally enable auto-send after navigation and verify one send for each actual navigation action. Regeneration resets position and never sends. Turn auto-send back off afterward.

13. **Finish observations and export.** Record retention after several minutes, dismissal behavior and overall practicality in notes. Save observations, leave full payload export off unless needed, then Export JSON report and choose a document destination. Open the JSON on a computer or JSON viewer; verify metadata, attempt IDs, modes, lengths and observations. Check titles/bodies are omitted when the option is off. Also cancel one export to verify local history remains. Preserve the report before uninstalling/clearing data or changing the session.

## Independent milestone decisions

| Milestone | Evidence required | Initial result |
| --- | --- | --- |
| Android app posts correctly | Build/install, permission and channel flows, expected phone content and IDs, reader controls, persistence and readable export | Pending device validation; build blocked in supplied workspace |
| Realme Link forwards this custom app | App present/selectable and identifiable tests actually observed on the real S2, with locked behavior measured separately | Pending physical S2 test |
| S2 display is practical for reading | At least one measured readable chunk budget, one delivery mode usable across multiple sections, retention/scrolling observations and tester acceptance of phone navigation | Pending physical S2 test |

If the first milestone passes and forwarding fails, preserve the evidence rather than changing to direct BLE or inventing a watch SDK. A negative or inconclusive compatibility result is still a useful prototype outcome. Only move to PDF/DOCX/PPTX extraction after the measured communication path is practical.
