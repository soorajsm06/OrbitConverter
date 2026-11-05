# Feature coverage — 0.1.0

“Implemented” describes code present in this build, not verification on all devices.
See TESTING.md for what was actually exercised. This is not all of Filee's features.

| Area | Orbit Convert implementation | Difference from Filee |
|---|---|---|
| Radial menu | Six slices per page, center to cycle, accessible action list | In-app touch UI, not desktop/global drag overlay |
| Hardware trigger | Hold either volume key for 450 ms after selection | Works only in this activity, not the system picker or another app |
| Inputs | System picker, multiple files, Android Share | No URL downloads, global drag interception or browser integration |
| Output | User-granted local folder, separate job folders, unique names | No arbitrary paths, cloud output, or overwrite mode |
| Images | JPG/PNG/WEBP output; Android-readable JPEG/PNG/BMP/GIF/HEIC inputs | HEIC depends on device; GIF first frame; no RAW/TIFF/AVIF writer |
| Image tools | Maximum width, quality, grayscale, fixed center crop | No interactive crop handles, rotation UI or complex image presets |
| Images → PDF | One image per page in selected order | Image-based PDF; no OCR |
| PDF → images | One PNG/JPG per selected page | Android renderer; no editable Office reconstruction |
| PDF → long image | One vertically stitched PNG | 20 MP and 30,000 px height cap |
| PDF tools | Merge, extract range, split, extract text | Password PDFs rejected by PDFBox tools; no OCR, signatures or form preservation guarantee |
| Rasterize PDF | New image-based PDF | Not guaranteed smaller; loses searchable text and interactive content |
| Plain text | UTF-8 text → PDF, basic DOCX, escaped HTML | Markup printed literally, not rendered as Markdown/HTML |
| DOCX | Body text extraction to TXT | No layout-faithful DOCX → PDF, embedded image conversion or full Office engine |
| Tables | CSV ↔ TSV; CSV/TSV → JSON objects; JSON object arrays → CSV; JSON pretty-print | Header validation; no XLSX/XLS/ODS; nested JSON is serialized in cells |
| Subtitles | SRT ↔ VTT timestamps and cue text | No ASS/SSA, TTML, style mapping or legacy encoding detection |
| Archives | Any selected files → ZIP; ZIP → flattened files | Size/count/path checks; no 7Z/RAR/TAR or encrypted ZIP |
| Media | AAC track → M4A without transcoding; first video frame → JPG | Container/codec/device dependent; no MP3 conversion or general FFmpeg feature parity |
| Batch | Sequential worker; selectable order; cancel; staged outputs | No concurrent conversion graph, background service or restart recovery |
| History | Last 30 output names/URIs; open/share; clear list | No thumbnails, automatic cleanup of exported files or global recent database |
| Privacy | No INTERNET permission, no login, no cloud output | Shared input provider can independently fetch its own cloud content |
| Themes/languages | One light English UI, Android system font | No Filee theme editor or translations |

## Not implemented

Full Office conversion (DOC/DOCX/XLS/XLSX/PPT/PPTX/ODF), HWP/HWPX, EPUB/MOBI/AZW,
font conversion, SVG/vector conversion, CAD, 3D models, geospatial formats, e-mail,
OCR, speech transcription, arbitrary audio/video transcoding, watch folders,
multi-step conversion routing, full preset editor, drag-and-drop profile editing,
automatic updates and global desktop gestures.

These are real engine-porting tasks, not hidden options. Unsupported types only get
“Create ZIP”; the app never pretends to convert by changing an extension.

## Path toward full parity

1. Define a fixed upstream version and a per-format corpus, including failure cases.
2. Port or replace the .NET document readers/writers and test document fidelity.
3. Build and audit Android-compatible native media/image/archive engines for each ABI;
   include all required codec/library licenses and test Android memory/page-size behavior.
4. Add OCR models as bundled optional variants if the app must remain fully offline.
5. Port specialist engines (CAD/3D/fonts/geospatial) with independent validation suites.
6. Add a foreground job service, restart behavior, advanced presets and watch scheduling.
7. Perform real-device volume-key/storage/provider tests and release-sign with the owner's key.

Global access to another file manager's selected URIs is not something a normal
Android app can obtain merely by listening to volume keys. Android Share remains
the supported cross-app bridge; full file-manager integration requires cooperation.
