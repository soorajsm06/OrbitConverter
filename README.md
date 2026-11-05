# Orbit Convert — Android pilot 0.1.0

An offline Android file converter inspired by Filee's radial-menu workflow.
**This is an initial Android implementation, not a feature-complete Filee port.**
The source includes an adapted Filee page-range parser; the Android interface and
engines are newly implemented. See [FEATURES.md](FEATURES.md) for the exact scope.

- App name: **Orbit Convert**
- Android application ID: `com.orbitconvert.app`
- Version name: `0.1.0`; version code: `1`
- Minimum Android: **8.0 (API 26)**; target/compile SDK: 35
- No login, Google Drive connection, analytics, ads, or INTERNET permission
- Original inputs are read only. Outputs go into a new local folder for each job.

## Install the supplied APK

1. Download `OrbitConvert-0.1.0-debug.apk` on your Android phone.
2. Open Downloads and tap the APK.
3. If Android asks, allow **Install unknown apps** for the browser or file manager
   you used. This is an APK installation setting, not a permission requested by Orbit.
4. Tap **Install**, then **Open**. You may disable that installer permission afterward.
5. This is a debug-signed testing APK, not a Play Store release. It is suitable for
   trying the pilot. Do not describe it as a production-certified or fully tested app.
6. If a later build says “App not installed” because its signing key differs,
   uninstall the old test build first. Exported documents remain in the chosen folder;
   uninstalling clears the app's recent list and saved folder grant.

## Convert your first file

1. Tap **Select files**. Browse **Internal storage** or your SD card. Long-press to
   select multiple files where your phone's picker supports it. Confirm selection.
2. Tap **Choose output folder**. Create a folder named `OrbitResults` under
   Documents or another permitted location, select it, and tap **Use this folder**.
   Android may prevent selecting the storage root, Downloads root, or Android/data.
   Choose a subfolder. Grant access when asked. This is Android's normal folder prompt.
3. Back on Orbit's main screen, **hold either Volume Up or Volume Down for 0.45 seconds**.
   The donut opens. Release the button, then tap the desired conversion.
4. Tap the donut's center for more actions. The buttons below the donut expose the
   same actions for accessibility. **Open conversion wheel** also works without keys.
5. Keep the app open while it converts. The result appears under **Recent results**.
   Tap a result to open or explicitly share it. It also exists in your output folder.
6. A short volume press still adjusts media volume. The long press only activates
   when files are selected and no job is running.

### From another file manager

Select files → **Share** → **Orbit Convert**. Then choose a local output folder if
needed and open the wheel. Orbit cannot read another app's private selected-file
state. The volume shortcut does **not** work globally inside Files, WhatsApp, or
other apps. No accessibility service or floating overlay is installed.

### A complete PDF as one image

Select a PDF → choose output folder → open wheel → **PDF to long image**.
All selected pages are stacked vertically into one PNG. In **Output settings**,
reduce width or specify a page range if the 20-megapixel / 30,000-pixel height limit
is exceeded. **PDF to PNG/JPG** instead writes one image per page.

### Merging, ordering and presets

Use the ↑ buttons beside files to change input order before **Merge PDF** or
**Images to PDF**. Output settings control width (100–4096 px), image quality
(10–100), page ranges (`1-3,5,8-`), and center-crop aspect ratio.
Ranges are sorted/deduplicated; reversed ranges are accepted; invalid ranges fail
explicitly instead of silently converting all pages. A range on a multi-PDF job
applies separately to every input.

## Privacy and limits

The APK has no network permission and performs no uploads. Use local sources;
a different file provider may itself download a cloud-backed item before handing it
to Orbit. Output is restricted to Android's local external-storage provider (internal
shared storage or SD card). No broad storage permission is requested.

Temporary copies live in the app-private cache and are deleted after success,
failure, or cancellation. Stale job folders are cleaned on the next launch after
process termination. Android can kill an app in the background: jobs do not resume.
A kill during the final save can leave a partial output job folder; remove that folder
and retry. Normal caught failures attempt to remove partially exported output.
Only the last 30 result names and document URIs are stored in app-local preferences.
Clear recent list removes those records, not the exported files.

Limits: 50 input files; 256 MiB per input; 512 MiB combined input; 8 MB text;
ZIP extraction at most 500 files, 128 MiB per entry, 512 MiB total. ZIP folders are
flattened using underscores and duplicate names get suffixes. Unsafe paths are rejected.
Decoded images are sampled to stay near 8 MP, output pages capped at 12 MP, and long
images capped at 20 MP. This protects phone memory but can reduce image resolution.
Only the first frame of animated images is used. HEIC input is device-dependent.
Password-protected PDFs, OCR, and preserving interactive PDF forms/signatures are
outside this pilot. Rasterizing PDF loses text, forms, links and vector quality;
it is not a guaranteed compression feature. Text input assumes UTF-8.
DOCX extraction reads body text; original formatting, headers, notes and pictures
are not preserved. Text-to-PDF treats markup as literal text.

## Upload the full project to GitHub and get another APK

The supplied source ZIP includes this whole project, Gradle wrapper, tests and
`.github/workflows/android.yml`. No repository has been created in your account.

1. Extract `OrbitConvert-0.1.0-source.zip` on your computer.
2. Create an empty GitHub repository named `OrbitConvert`.
3. Upload the **contents** of the extracted `OrbitConvert` directory to the repository
   root, including `.github`, `gradle`, `gradlew`, `gradlew.bat`, and `app`.
   Do not upload `local.properties`, build folders, private signing keys, or `.gradle`.
   GitHub's web uploader can omit hidden folders; Git is the reliable route:

   ```sh
   cd OrbitConvert
   git init
   git add .
   git commit -m "Initial Orbit Convert Android pilot"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/OrbitConvert.git
   git push -u origin main
   ```

4. Open the repository's **Actions** tab. Select **Build Android APK**.
5. Run the workflow, or wait for the `main` push build. It runs unit tests, lint and APK compilation.
6. Open the successful run and download **OrbitConvert-0.1.0-debug-APK** under Artifacts.
7. Extract that artifact ZIP. `app-debug.apk` is the installable application.
   Artifact downloads generally require GitHub login; the installed app never does.
8. For a public APK download link, create a GitHub Release tagged `v0.1.0`, and attach
   the APK as a release asset. The workflow deliberately does not publish automatically.

## Build on your computer with Android Studio

1. Install Android Studio and let its setup wizard install the Android SDK.
2. Extract the source ZIP and open the `OrbitConvert` folder in Android Studio.
3. Set Gradle JDK to Java 17 (Settings → Build, Execution, Deployment → Build Tools → Gradle).
4. In SDK Manager install Android SDK Platform 35 and accept the SDK licenses.
5. Allow Gradle sync to finish. The first build downloads libraries, requiring internet
   on the computer. The app itself does not need internet.
6. Build APK from Android Studio's Build menu, or run:

   Windows PowerShell:
   ```powershell
   .\gradlew.bat testDebugUnitTest lintDebug assembleDebug
   ```

   macOS/Linux:
   ```sh
   chmod +x gradlew
   ./gradlew testDebugUnitTest lintDebug assembleDebug
   ```

7. Find the APK at `app/build/outputs/apk/debug/app-debug.apk`.
8. For a public production release, use Build → Generate Signed App Bundle / APK.
   Create and securely back up your own release keystore. Keep that same private key
   for future updates. Never upload it to a public repository. Debug APKs do not use it.
9. For updates, increase `versionCode` in `app/build.gradle` and change `versionName`.
   Update the workflow artifact label if desired. Test the new APK on actual phones.

## Project map

- `MainActivity.java`: Android picker, folder grants, share intake, volume gesture,
  job execution, output commit, history and interface
- `RadialMenu.java`: touch donut with six actions per page and center pagination
- `Formats.java`: capability filtering (mixed unsupported selections expose ZIP only)
- `ConvertEngine.java`: image/PDF/text/archive/table/subtitle/media operations
- `PageRange.java`: adapted Filee parser with strict validation
- `app/src/test`: behavior tests and Robolectric launch test
- `FEATURES.md`: coverage and missing upstream features
- `UPSTREAM_REVIEW.md`: inspected repository and porting decisions
- `TESTING.md`: actual validation evidence and device checks still required

## License

New code is MIT. The adapted Filee logic retains its MIT copyright notice.
PDFBox Android and AndroidX have their own Apache-2.0 notices, included in the app.
Filee's desktop engines, original branding and signing identity are not bundled.

## Rendered app preview

These are automated Android renders, not photos from a physical phone.

![Home](docs/screenshots/home.png)

![Conversion wheel](docs/screenshots/wheel.png)
