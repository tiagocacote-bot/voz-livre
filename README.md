# Voz Livre — Free Voice

**Free, open-source augmentative and alternative communication (AAC) for Android.**

[![Download the Android APK](https://img.shields.io/badge/Download-Android%20APK-2ea44f?style=for-the-badge&logo=android&logoColor=white)](https://github.com/tiagocacote-bot/voz-livre/releases/latest/download/Voz-Livre.apk)

[Português do Brasil](README.pt-BR.md) · [All releases](https://github.com/tiagocacote-bot/voz-livre/releases) · [Build from source](BUILDING.md)

Communication should not depend on income or device brand. Voz Livre is a free AAC app for Android, created to expand access for autistic people, other neurodivergent people and anyone who needs support to communicate — especially families who cannot afford an iPhone or iPad and only have Android devices. It offers a customizable communicator for affordable phones and tablets. It is and will stay free: no account, subscription or ads.

**0.2.0 is an alpha.** The app and starter boards are currently in **Brazilian Portuguese**; this English README does not mean the interface is translated. The project owner reports that basic use of the previous version appeared to work on a Samsung Galaxy S24 FE and a Samsung SM-P619 tablet; app version, Android versions and tested features were not recorded. Broader accessibility testing is still needed. This is an independent project, not TD Snap, and is not affiliated with Tobii Dynavox. It does not reproduce all TD Snap features.

## Try it

1. Tap the **Download** button above. The link always serves the newest release (`Voz-Livre.apk`).
2. On Android 8.0 or newer, open the APK and allow installation from the app you used to download it. An existing installation updates in place and keeps its data.
3. Install/download a Brazilian Portuguese voice in Android's text-to-speech settings for offline speech. Boards and local editing work offline; speech depends on your installed voice engine.
4. Review the boards, symbols and access settings with the person using them. Export a backup from **Ajustes** before substantial changes.

## Available in the alpha

- Eight starter pages and 166 buttons with pictograms and text; compose a message and speak it aloud.
- **Word prediction:** four next-word suggestions, also in the keyboard. Starts from Portuguese starter suggestions and learns, on the device only, from the messages each profile speaks. It can be turned off or cleared.
- Edit pages, labels, spoken messages, colors, links and visibility; add personal photos through the system picker.
- Separate local profiles, search, a keyboard, notes, text-based routines, timer and drawing.
- Direct touch, hold-to-select, selection on release, auditory cues, pointer dwell and linear automatic/step scanning. These implementations still need testing with users and hardware.
- Local JSON backup/restore including custom photos, message undo/redo and page edit undo.
- Experimental `.spb`/`.sps` board import: a limited subset of text, page links and supported commands. Unsupported actions are disabled for review. Import does **not** preserve every layout, image or advanced action.

## Pictures

Starter buttons use **ARASAAC pictograms**, as in the original prototype (163 of 166 buttons; three keep Mulberry or original drawings where no suitable ARASAAC pictogram exists). Updating from 0.1.1 switches the starter buttons back to ARASAAC automatically, keeping personal photos and hand-picked symbols. Settings can switch the starter buttons back to Mulberry and original drawings; both sets remain available in the symbol picker.

## Current limits

There is no calibrated eye tracking, cloud sync, full grammatical prediction, home automation, Boardmaker activities or commercial PCS/PODD/Gateway vocabulary. Scanning does not cover every dialog and editing screen. Android Switch Access compatibility, external switches, pointer access and speech engines need physical-device validation. Automated tests are not evidence of clinical suitability or safe reliance as someone's only means of communication. Vocabulary, suggestions and symbol mappings are an editable starting point, not a validated AAC vocabulary.

![Voz Livre — Android](docs/images/paisagem.png)

Screenshot rendered in a simulated Android API 35 environment, not a physical device.

## Privacy

No analytics, ads, login or app server. The manifest requests no Internet, microphone or camera permission. Photos use the Android picker. Boards, profiles, learned suggestion words and personal photos stay in app storage until you choose to export or share them. Android's TTS engine and share targets have their own policies. Backups may contain personal communication and photos; keep them somewhere you trust. See [privacy and security](PRIVACY.md).

## Help improve access

AAC users, autistic people, families, speech-language professionals, translators, designers and Android developers are welcome. Useful contributions include testing on affordable devices, reporting access barriers, improving vocabulary with users, translation and accessible input. See [CONTRIBUTING.md](CONTRIBUTING.md). Please do not put private family data, children's photos or unredacted backups in public issues.

## License and credits

- Application code, documentation and original drawings: [MIT](LICENSE).
- **ARASAAC pictograms**: author Sergio Palao. Origin: [ARASAAC](https://arasaac.org). License: [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/). Property of the Government of Aragón (Spain). **Non-commercial use only.**
- **Mulberry Symbols by Steve Lee**: [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). The bundled SVG sources and PNG conversions retain attribution and share-alike obligations.
- [Attribution, source versions and modifications](THIRD_PARTY_NOTICES.md); per-symbol hashes and origins in [`symbols-provenance.json`](app/src/main/assets/symbols-provenance.json).

Voz Livre is distributed free of charge. Because the ARASAAC pictograms do not allow commercial use, the app with these pictures must not be sold or used in a commercial product; the MIT code can be reused separately. No proprietary TD Snap code, voices, PCS, PODD or Gateway symbols are bundled.
