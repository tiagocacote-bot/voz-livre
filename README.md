# Voz Livre

**Free, open-source augmentative and alternative communication (AAC) for Android.**

[Português do Brasil](README.pt-BR.md) · [Download the Android APK](https://github.com/tiagocacote-bot/voz-livre/releases) · [Build from source](BUILDING.md)

Communication should not depend on income or device brand. Voz Livre aims to make a customizable communicator available on affordable Android phones and tablets, including for autistic people and other people who use AAC. Free to use, study, adapt and share; no account, subscription or ads.

**0.1.1 is an early alpha.** The app and starter boards are currently in **Brazilian Portuguese**; this English README does not mean the interface is translated. The project owner reports that basic use appeared to work on a Samsung Galaxy S24 FE and a Samsung SM-P619 tablet. App version, Android versions and tested features were not recorded; this does not confirm every access mode or the new 0.1.1 artwork edition. Broader accessibility testing is still needed. This is an independent project, not TD Snap, and is not affiliated with Tobii Dynavox. It does not reproduce all TD Snap features.

## Try it

1. Download `Voz-Livre-0.1.1.apk` from the [alpha release](https://github.com/tiagocacote-bot/voz-livre/releases/tag/v0.1.1).
2. On Android 8.0 or newer, open the APK and allow installation from the app you used to download it. Availability of each access method depends on the device and peripherals.
3. Install/download a Brazilian Portuguese voice in Android's text-to-speech settings for offline speech. Boards and local editing work offline; speech depends on your installed voice engine.
4. Review the boards, symbols and access settings with the person using them. Export a backup from **Ajustes** before substantial changes.

## Available in the alpha

- Eight starter pages and 166 buttons with pictures and text; compose a message and speak it aloud.
- Edit pages, labels, spoken messages, colors, links and visibility; add personal photos through the system picker.
- Separate local profiles, search, a keyboard, notes, text-based routines, timer and drawing.
- Direct touch, hold-to-select, selection on release, auditory cues, pointer dwell and linear automatic/step scanning. These implementations still need testing with users and hardware.
- Local JSON backup/restore including custom photos, message undo/redo and page edit undo.
- Experimental `.spb`/`.sps` board import: a limited subset of text, page links and supported commands. Unsupported actions are disabled for review. Import does **not** preserve every layout, image or advanced action.

## Current limits

There is no calibrated eye tracking, cloud sync, predictive grammar, home automation, Boardmaker activities or commercial PCS/PODD/Gateway vocabulary. Scanning does not cover every dialog and editing screen. Android Switch Access compatibility, external switches, pointer access and speech engines need physical-device validation. Automated tests are not evidence of clinical suitability or safe reliance as someone's only means of communication.

The symbol mappings are an editable starting point, not a validated AAC vocabulary. Version 0.1.1 replaces the private prototype's non-commercial symbol assets with Mulberry and original artwork. **Pictures change:** review familiar buttons before an existing user switches to this edition. Old numeric filenames remain as aliases containing the new artwork.

![Voz Livre — Android](docs/images/paisagem.png)

Screenshot rendered in a simulated Android API 35 environment, not a physical device.

## Privacy

No analytics, ads, login or app server. The manifest requests no Internet, microphone or camera permission. Photos use the Android picker. Boards, profiles and personal photos stay in app storage until you choose to export or share them. Android's TTS engine and share targets have their own policies. Backups may contain personal communication and photos; keep them somewhere you trust. See [privacy and security](PRIVACY.md).

## Help improve access

AAC users, autistic people, families, speech-language professionals, translators, designers and Android developers are welcome. Useful contributions include testing on affordable devices, reporting access barriers, improving vocabulary with users, translation and accessible input. See [CONTRIBUTING.md](CONTRIBUTING.md). Please do not put private family data, children's photos or unredacted backups in public issues.

## License and credits

- Application code, documentation and original drawings: [MIT](LICENSE).
- **Mulberry Symbols by Steve Lee**: [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). The bundled SVG sources and PNG conversions retain attribution and share-alike obligations. This artwork license is separate from the MIT code license.
- [Attribution, source versions and modifications](THIRD_PARTY_NOTICES.md); per-symbol hashes and origins in [`symbols-provenance.json`](app/src/main/assets/symbols-provenance.json).

No proprietary TD Snap code, voices, PCS, PODD or Gateway symbols are bundled. The project is free to distribute under the licenses above, including commercial redistribution when their conditions are met.
