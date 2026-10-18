# Alpha validation — 0.3.0 (2026-10-18)

- GitHub Actions (Windows) on every push: asset checks (317 buttons, 378 PNGs including 244 ARASAAC pictograms, licenses and hashes, migration tables, English/Spanish boards in sync, every interface string translated), 67 model/grammar/prediction/migration/i18n assertions, APK build, and the Robolectric runtime suite on APIs 28 and 35 — now including dialog scanning, interface language, word forms, routines and an improved TD Snap import check — plus render checks.
- Not yet tested on a physical device: scanning inside dialogs with real switches, Spanish/English voices, the 0.2.0 → 0.3.0 page upgrade. Please report results (device, Android version, access method).

## Previous: 0.2.0 (2026-10-18)

- GitHub Actions (Windows) on every push: asset checks (166 buttons, 275 PNGs including 141 ARASAAC pictograms, per-record licenses and SHA-256 values, migration tables, version consistency), 37 model/scanner/prediction/migration assertions, APK build with OpenJDK 17, Android API 35, AAPT2 8.7.3, D8 8.7.18 and apksig 8.7.3, and — new in this version — the Robolectric runtime suite on APIs 28 and 35 plus native-graphics render checks.
- Release APKs are built by the release workflow, signed with the same key as 0.1.1 (the workflow compares certificates and refuses a mismatch) and listed with SHA-256 values in each release's `SHA256SUMS.txt`.
- The images in `docs/images` are simulated screenshots rendered by the CI and include attributed ARASAAC pictograms.
- Not yet tested on a physical device: ARASAAC migration of an existing 0.1.1 installation, word suggestions and in-place update from 0.1.1. Please report results (device, Android version, access method).

The limitations below still apply.

---

## Previous: 0.1.1 (2026-10-17)

Published APK: `Voz-Livre-0.1.1.apk`, SHA-256 `676d613c7826604cd9abc73752fec03e1b58a141373f9e729e818ad7d1212536`.

- Windows custom build completed with OpenJDK 17, Android API 35, AAPT2 8.7.3, D8 8.7.18 and apksig 8.7.3. APK signature verification succeeded (v2 and v3).
- Manifest: package `br.pessoal.vozlivre`, version code 2, min SDK 26, target SDK 35, no requested app permissions.
- 22 model/scanner assertions passed using the public starter board.
- 24 Robolectric test cases passed: 11 runtime cases on each of APIs 28 and 35, plus two API 35 native-graphics rendering cases.
- Asset checks passed for 166 buttons, 272 PNG files (134 distinct symbol sources plus 138 legacy aliases), license records and source/PNG SHA-256 values.
- Initial, food and landscape render images were visually inspected. The images in `docs/images` are simulated screenshots and include attributed Mulberry artwork.

The project owner reports basic operation apparently succeeded on a Samsung Galaxy S24 FE and a Samsung SM-P619 tablet. App build, Android versions and feature coverage were not recorded; this report does not confirm the new 0.1.1 artwork edition. Real speech engines, Bluetooth/USB switches, external pointer hardware, screen readers and real-world AAC use still need targeted validation. Tests do not establish clinical effectiveness or full TD Snap compatibility. Starter labels and symbol mappings need individual review. CI runs build/model/asset checks; the runtime suite is documented separately in BUILDING.md.
