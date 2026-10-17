# Build and test

## Tested Windows build

Requirements: Windows x64, Node.js 24, Python 3 and PowerShell. From the repository root:

```powershell
node setup-tools.mjs
./tests/run-model.ps1
python scripts/check-assets.py
./build.ps1
```

`setup-tools.mjs` downloads the tools listed in `tools.lock.json`, checks each SHA-256 and extracts OpenJDK 17 and AAPT2 into ignored `.toolchain/`. Downloads are development-only. Android platform stubs are pinned to API 35. This build does not need Android Studio or Gradle; it runs AAPT2, Java compilation, D8, Python packaging and apksig. Native code is not bundled.

The signed APK is `build/Voz-Livre-0.1.1.apk`. The first build creates your own private signing key and password in ignored `signing/`. Keep those private and backed up if you want to ship updates with the same identity. **Do not commit or upload this directory.** CI uses its own ephemeral key, so its APK cannot update a release signed with a different key.

To reuse tools or a private signing directory outside the repository:

```powershell
./build.ps1 -ToolsDirectory C:/dev/android-tools -SigningDirectory C:/private/voz-livre-signing -PythonPath python
```

## Android Studio alternative

The project also includes standard Gradle configuration: AGP 8.7.3, JDK 17, compile/target SDK 35, minimum SDK 26, Java 8 source. Open the root in Android Studio and install SDK 35. A Gradle wrapper is not included; use a compatible Gradle 8.9 installation or your IDE. This alternative was not used to produce the published alpha APK.

## Runtime regression tests

`tests/RuntimeTest.java` runs 11 checks on each of API 28 and 35; `RenderTest.java` adds two API 35 native-graphics render checks. Robolectric is a JVM simulation, not a physical device. Java 17 is required for this test setup. A Windows shadow emulates AtomicFile's replace behavior for the test host filesystem.

After building, resolve test-only Maven dependencies into `build/robo-deps`:

```powershell
mvn -f tests/pom.xml dependency:copy-dependencies -DincludeScope=test -DoutputDirectory="$PWD/build/robo-deps"
```

Extract each downloaded `.aar` as a ZIP and copy its `classes.jar` into `build/robo-deps` under a unique `*-classes.jar` name. Keep the original JARs too. Download the optional public SPB fixture from [AACProcessors](https://raw.githubusercontent.com/willwade/AACProcessors/9e538963b236b84afce1ecf94980506d9a9bcd64/examples/demofiles/Medical%20Advocacy.spb) to `.test-fixtures/sample.spb`. Verify its SHA-256: `d2d6895d4fdc3c258eb971c05330cf34e446ec25da016643f3dde26ba0c98836`. Do not bundle it into the app.

```powershell
./tests/run-runtime.ps1
```

Use a short checkout path on Windows: native SQLite test files can exceed Windows path limits. The runner creates its temporary `rt` directory beside the checkout. The `-ToolsDirectory`, `-DependenciesDirectory` and `-SampleFile` parameters support an existing development environment.

## Artwork sources

Bundled PNGs and SVGs are ready to use; no graphics package is required to build the APK. To edit a symbol, edit its SVG in `artwork/`, then run `npm install` and `npm run symbols`. Sharp 0.35.5 is a development dependency. The renderer updates aliases and provenance hashes; review the visual changes and preserve the per-record licenses. New symbol mappings also require updating the board and provenance records.

## Português

No Windows x64, instale Node.js 24 e Python 3 e execute os quatro comandos da primeira seção. O APK vai para `build/`. As ferramentas são verificadas por hash e ficam em `.toolchain/`. A chave e a senha de assinatura ficam em `signing/`: não publique esses arquivos. Cada chave identifica uma linha de atualizações diferente.

As imagens já estão prontas para compilar. Os SVGs editáveis ficam em `artwork/`; preserve os créditos e a licença de cada imagem. Os testes de modelo, ativos e simulação Android não substituem testes no aparelho e com a pessoa que utilizará CAA.
