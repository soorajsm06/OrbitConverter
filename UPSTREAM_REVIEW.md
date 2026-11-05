# Filee source review and Android adaptation

Repository: https://github.com/KnifeLemon/Filee
Inspected commit: `da4bf57f5c099ea83956b1d274dbef255e44203e`
Review date: 2026-11-04

Reviewed the root MIT license, README feature/format list, docs/ARCHITECTURE.md,
docs/ENGINES.md, src/Filee.Core/Conversion/PageRange.cs, and the source layout of
Filee.App, Filee.Core, Filee.Engines and platform projects.

Filee is an Avalonia/.NET desktop application. Its global input hooks lead into a
radial controller; its job queue routes conversions through engine edges. Engines
include .NET readers/writers plus native or external tools such as ImageMagick,
PDFium, rhwp, FFmpeg, LibreOffice, Pandoc, Ghostscript, calibre and 7-Zip.
The repository has Windows/macOS/Linux platform projects, not an Android APK target.

The desktop application's input hooks and packaged native executables cannot simply
be placed in an Android APK. Android uses sandboxed content URIs, grants access via
Storage Access Framework, and dispatches ordinary key events to the active window.
Orbit therefore implements a native Java activity and Android conversion adapters.

Concrete code reuse: PageRange.java adapts the parsing/normalization behavior in
Filee's PageRange.cs. Changes: strict invalid/range rejection, Java collections and
exceptions, and no silent fallback to every page for malformed input. The original
copyright and MIT license are retained. Other conversion implementations are new;
this is an adaptation inspired by Filee, not a claim that all upstream code was ported.

Dependencies are pinned in app/build.gradle. The original repository itself is not
included wholesale because most of it does not build for Android and would obscure
which code actually runs in this APK. Revisit this review against a new upstream
commit before claiming parity with a future Filee release.

Reference documentation:
- https://developer.android.com/training/data-storage/shared/documents-files
- https://developer.android.com/reference/android/view/KeyEvent
- https://github.com/TomRoush/PdfBox-Android
