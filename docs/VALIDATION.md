# Alpha validation — 2026-10-17

Published APK: `Voz-Livre-0.1.1.apk`, SHA-256 `676d613c7826604cd9abc73752fec03e1b58a141373f9e729e818ad7d1212536`.

- Windows custom build completed with OpenJDK 17, Android API 35, AAPT2 8.7.3, D8 8.7.18 and apksig 8.7.3. APK signature verification succeeded (v2 and v3).
- Manifest: package `br.pessoal.vozlivre`, version code 2, min SDK 26, target SDK 35, no requested app permissions.
- 22 model/scanner assertions passed using the public starter board.
- 24 Robolectric test cases passed: 11 runtime cases on each of APIs 28 and 35, plus two API 35 native-graphics rendering cases.
- Asset checks passed for 166 buttons, 272 PNG files (134 distinct symbol sources plus 138 legacy aliases), license records and source/PNG SHA-256 values.
- Initial, food and landscape render images were visually inspected. The images in `docs/images` are simulated screenshots and include attributed Mulberry artwork.

The project owner reports basic operation apparently succeeded on a Samsung Galaxy S24 FE and a Samsung SM-P619 tablet. App build, Android versions and feature coverage were not recorded; this report does not confirm the new 0.1.1 artwork edition. Real speech engines, Bluetooth/USB switches, external pointer hardware, screen readers and real-world AAC use still need targeted validation. Tests do not establish clinical effectiveness or full TD Snap compatibility. Starter labels and symbol mappings need individual review. CI runs build/model/asset checks; the runtime suite is documented separately in BUILDING.md.
