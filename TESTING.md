# Validation — Android pilot 0.1.0

Validation date: 2026-11-04.
This report distinguishes automated checks from real-phone validation.

## Automated coverage

The project contains 23 JUnit/Robolectric checks:

- Page ranges: Unicode digits/dashes/commas, open ranges, reversed ranges,
  deduplication, empty/all pages, out-of-range and malformed selections.
- Capability filtering: PDF actions, mixed types and unsupported formats.
- CSV quoted multiline round trip, string values preserved in JSON, duplicate
  headers rejected.
- ZIP round trip and path traversal rejection; stream-size limits.
- Basic DOCX text round trip, including XML-sensitive characters.
- PDF split/merge with two different page sizes and original text preservation.
- VTT timestamp conversion to SRT.
- Activity launch without an account prompt, long-volume gesture and reopening.
- Native graphics JPEG encoding (real file signature and resized dimensions).
- Rendered home and donut screens for visual inspection.

Robolectric uses simulated Android API 28 with native graphics for image tests.
These tests do not constitute installation or end-to-end testing on a physical phone.
The donut rendering was visually inspected and density-dependent line spacing fixed.

Build commands: `./gradlew clean testDebugUnitTest lintDebug assembleDebug`.
APK checks: `apksigner verify --verbose`, `aapt dump badging`, and
`aapt dump permissions`. The build's manifest must have no requested permissions.
The supplied APK is debug-signed; it is not a production release.

## Checks still required on real Android devices

- Install/launch on Android 8, 12, 15 and newer devices; verify edge-to-edge insets.
- Hold both volume keys individually, release, reopen, and confirm short presses
  still control media volume. Test while a headset or external keyboard is connected.
- Pick local files, cancel pickers, select multiple items, revoke folder access,
  restart the app and re-grant access.
- Share one/multiple files from Files and a gallery application.
- Render PDFs to separate images and long images; test scans, rotated pages,
  mixed page sizes, missing fonts, malformed PDFs, and large/page-range failures.
- Validate searchable PDF extraction with non-Latin content and unusual fonts.
- Test HEIC, EXIF orientations, alpha images, animated-image first frame,
  center crop and grayscale.
- Verify AAC extraction and video frames with actual supported device codecs.
- Confirm cancellation, storage-full behavior, SD-card removal and process-kill
  handling. A process kill during output commit can leave a partial job folder.
- Check TalkBack, large-font settings, landscape orientation and small screens.
- Review dependencies and signing policy before a production distribution.

No emulator system image or physical Android device was available in this environment.
No claim is made that every listed conversion has been exercised end to end on hardware.

## Observed final results

Clean build succeeded. **23 tests passed, zero failures/errors.** Android lint: **0 errors, 15 warnings**. APK signature verification passed (v2). APK manifest inspection found no requested permissions.

Remaining lint warning categories: GradleDependency (2), TrustAllX509TrustManager (3), DataExtractionRules (1), ViewConstructor (1), SetTextI18n (8). These include pinned dependency updates, English-only UI strings, a programmatic custom-view constructor, and unused trust-manager code inside the PDF library dependency. The app has no network permission and does not instantiate that networking code. Warnings have not been hidden with a baseline.
