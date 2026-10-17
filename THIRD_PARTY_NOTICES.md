# Artwork and third-party notices

## Mulberry Symbols

**Mulberry Symbols by Steve Lee**. Copyright 2018–2026 Steve Lee.

Source: https://github.com/mulberrysymbols/mulberry-symbols

Pinned release: v3.6.1, commit `9cbab9f400c5de44e2bc58839cca07294aadb086`.

License: Creative Commons Attribution-ShareAlike 4.0 International, https://creativecommons.org/licenses/by-sa/4.0/ (legal text: https://creativecommons.org/licenses/by-sa/4.0/legalcode.en).

Changes: selected SVGs were renamed and rendered to transparent 256×256 PNGs; some PNGs are duplicated under legacy numeric names for backup compatibility. SVG source files are included in `artwork/`. The selection and Portuguese labels are specific to Voz Livre and are not endorsed by the original symbol creators. Original upstream license notice: `artwork/LICENSE-MULBERRY.txt`.

The CC BY-SA license applies to these images and their derivatives, not to the independently written MIT application code. Preserve attribution, license information and notice of modifications when sharing the images.

## Original artwork

Files named `original-*.svg` and their PNGs are original geometric drawings by Voz Livre contributors, licensed under the root MIT license. They are editable starting points, not clinically validated symbols. Per-record attribution, source paths and SHA-256 hashes are recorded in `app/src/main/assets/symbols-provenance.json`.

## Development dependencies

The application has no third-party Java runtime libraries bundled. Development tools are downloaded separately, with URLs and hashes in `tools.lock.json`. Android platform/build tools, OpenJDK, Maven test libraries and Sharp keep their respective upstream licenses; they are not part of the distributed application source or APK as standalone tools. Tests use Robolectric 4.17, JUnit 4.13.2 and JSON-java 20240303.

The optional import test downloads a public sample from AACProcessors, pinned at commit `9e538963b236b84afce1ecf94980506d9a9bcd64`. It is a test fixture only, not bundled with the application or this source distribution. No proprietary vocabulary is provided.
