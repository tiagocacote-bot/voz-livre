# Voz Livre — contexto completo para continuar o projeto (handoff para IA)

> Cole este arquivo inteiro no início da conversa com a nova IA. Ele descreve o projeto, o estado atual (18 de outubro de 2026, versão 0.3.1 publicada), as decisões tomadas, como compilar/testar/publicar, as restrições legais e o que falta fazer.

---

## 1. O que é o projeto

- **Nome:** Voz Livre — Free Voice.
- **O quê:** aplicativo Android gratuito e de código aberto de **CAA (Comunicação Aumentativa e Alternativa)**, um comunicador por pranchas de pictogramas com voz.
- **Para quem:** pessoas autistas, outras pessoas neurodivergentes e qualquer pessoa que precise de apoio para se comunicar — especialmente famílias brasileiras que **não podem comprar iPhone/iPad** e só têm Android barato.
- **Missão do dono:** o app **será sempre 100% gratuito, sem anúncios, sem conta, sem assinatura, open source**. Não há nenhuma intenção comercial.
- **Inspiração:** o app comercial TD Snap (Tobii Dynavox). O Voz Livre é independente, **não** tem vínculo com a Tobii Dynavox e não copia código, símbolos ou vocabulários proprietários.
- **Repositório:** https://github.com/tiagocacote-bot/voz-livre (público). Conta dona: `tiagocacote-bot`. O dono também tem a conta GitHub `tiagoealanagpt-create` (não é dona do repo).
- **Idioma de trabalho com o dono:** português do Brasil, linguagem simples. O dono não é programador: dê passos de clique em clique quando ele precisar fazer algo no site do GitHub ou no PowerShell.
- **Ícone (0.3.1):** balão de fala branco com o símbolo do infinito nas cores do arco-íris (neurodiversidade) sobre fundo verde-azulado `#087F8C`. A peça de quebra-cabeça foi evitada de propósito (muitas pessoas autistas a rejeitam). Ícone adaptativo em `res/drawable/icon*.xml`; PNG da loja gerado por `scripts/render-icon.py`.
- **Aparelhos de teste do dono:** Samsung Galaxy S24 FE e tablet Samsung SM-P619. Uso básico foi relatado como funcionando em versões anteriores; a 0.3.x **ainda não foi testada em aparelho físico**.

## 2. Estado atual

- **Versão publicada:** 0.3.1 (release `v0.3.1`, versionCode 5) — https://github.com/tiagocacote-bot/voz-livre/releases/tag/v0.3.1. Tudo está na `main`; não há PR aberto.
- **PRs já mergeados:** #1 (0.2.0), #2 (0.3.0), #3 (0.3.1: ícone, Galaxy Store, este documento).
- **Atualização no celular não é automática:** o app não tem permissão de internet (privacidade), então não avisa sobre versão nova. Quem instalou pelo GitHub precisa baixar o APK de novo; pela Galaxy Store a atualização seria automática.
- **Link fixo de download (sempre a release mais recente):** https://github.com/tiagocacote-bot/voz-livre/releases/latest/download/Voz-Livre.apk
- **Histórico de versões:**
  - 0.1.0 — protótipo privado (nunca publicado) com pictogramas ARASAAC nomeados pelo ID numérico (`symbols/6632.png` = "eu").
  - 0.1.1 — primeira versão pública: trocou ARASAAC por Mulberry Symbols e desenhos próprios (para permitir uso comercial). Manteve os nomes numéricos antigos como "aliases".
  - 0.2.0 — **voltou aos pictogramas ARASAAC originais** (pedido do dono), migração automática, previsão de palavras, release assinada com link fixo.
  - 0.3.0 — varredura dentro das janelas, rotinas com pictogramas, gramática portuguesa, interface e pranchas em EN/ES, vocabulário aberto (Núcleo, Começar a falar), 5 páginas temáticas, importação TD Snap melhorada.
  - 0.3.1 — novo ícone (balão de fala + infinito arco-íris), materiais da Galaxy Store e este documento de contexto.

## 3. Arquitetura (Java puro, sem bibliotecas)

App Android em **Java 8 (source)**, sem Gradle no build oficial, sem AndroidX, sem bibliotecas de terceiros em runtime. A interface é toda construída por código (sem layouts XML). minSdk 26 (Android 8), compile/target SDK 35. Pacote `br.pessoal.vozlivre`. **Nenhuma permissão** no manifesto (nem Internet).

| Arquivo | Linhas aprox. | Papel |
|---|---|---|
| `app/src/main/java/br/pessoal/vozlivre/MainActivity.java` | ~2900 | Toda a interface: grade de botões, barra de mensagem, fala (TextToSpeech), métodos de acesso (toque, manter, soltar, pista auditiva, ponteiro, varredura), varredura dentro de diálogos (com grupos de 8), editor de botões/páginas, perfis, ajustes, teclado, notas, rotinas, temporizador, quadro branco, backup/restauração, idioma, sugestões, formas das palavras. **Arquivo com CRLF.** |
| `Model.java` | ~400 | Dados (JSON): perfis, páginas, mensagem, desfazer/refazer, validação de segurança, `switchSymbols` (ARASAAC↔Mulberry), `upgradeStarter` (adiciona páginas novas uma vez). Sem dependência de Android (testável na JVM). |
| `Repository.java` | ~220 | Salvar/carregar com AtomicFile, recuperação, backup com fotos (base64), `defaults(lang)` (prancha pt/en/es), `upgrade()` (migrações ao abrir/restaurar). |
| `Predictor.java` | ~150 | Previsão de palavras local: contagens de palavras e pares por perfil (`profile.usage`), sementes por idioma. |
| `Grammar.java` | ~335 | Gramática pt-BR: conjugação (presente, passado, imperfeito, futuro, gerúndio, particípio; regulares, mudanças ortográficas, ~30 irregulares), reconhecimento de forma conjugada → infinitivo, gênero/número, plurais especiais. |
| `I18n.java` | ~50 | Tradução: chave = texto em português; dicionários `assets/i18n/en.json` e `es.json`. `t("...")` em MainActivity; `I18n.t` nas outras classes. |
| `SnapImporter.java` | ~355 | Importação read-only de pranchas TD Snap `.spb/.sps` (SQLite): páginas, textos, links, ordem pela grade (layout da página), cores, ocultos, **fotos da própria prancha** (`PageSetData` com `IMG:`). Símbolos de biblioteca (`SYM:`, ex.: PCS) **não** são copiados (proprietários). Nunca executa ações. |
| `ScanEngine.java` | 25 | Estado da varredura linear. |
| `BuildInfo.java` | — | `VERSION` (deve bater com manifesto/gradle/package.json; o check confere). |
| `res/drawable/icon.xml` + `icon_background.xml` + `icon_foreground.xml` | — | Ícone adaptativo (0.3.1). |

### Assets (`app/src/main/assets/`)
- `default.json` — prancha inicial em português (15 páginas, 317 botões, 2 rotinas). `default-en.json` / `default-es.json` — **gerados** por `scripts/build-boards.py` a partir de `scripts/board-translations.json` (não edite à mão).
- `symbols/` — 378 PNG 256×256: 244 ARASAAC (`<id>.png`), Mulberry (`mulberry-*.png`), desenhos próprios (`original-*.png`).
- `symbols-provenance.json` — um registro por PNG: provedor, licença, autor, URL, SHA-256 (o check confere hashes).
- `legacy-symbols.json` — mapa Mulberry/original ↔ ARASAAC por botão (`byButton`), por símbolo (`bySymbol`), `since030` (botões que ganharam ARASAAC na 0.3.0) e `pages030` (páginas novas a adicionar a quem atualiza).
- `prediction.json`, `prediction-en.json`, `prediction-es.json` — sementes de "próxima palavra".
- `i18n/en.json`, `i18n/es.json` — traduções da interface.
- `artwork/` (raiz do repo) — SVGs editáveis Mulberry e próprios (bytes preservados; `.gitattributes` com `-text`).

### Formato dos dados (JSON salvo no aparelho e no backup)
`{format:"voz-livre", version:1, symbolSet:"arasaac"|"mulberry", starterVersion:2, uiLanguage:"auto"|"pt"|"en"|"es", activeProfile, profiles:[{id, name, home, settings:{method, holdMs, scanMs, scanType, columns, font, rate, pitch, speakWords, contrast, predict, grammarAuto, language, voice, pin}, pages:[{id, title, columns, fixedLayout?, buttons:[{id, label, message, type:"word"|"speak"|"link"|"wordlink", target?, color, symbol, hidden, disabled?}]}], message:[{text, symbol}], notes, routines:[{title, done, steps:[{text, symbol}] }], usage:{w:{}, b:{}} }]}`
- Caminhos de imagem só podem ser `symbols/…` ou `user/…` (validado contra path traversal).
- `wordlink` = acrescenta o texto e abre a página `target` (usado nos inícios de frase).
- `fixedLayout:true` = página com colunas próprias (Núcleo usa 6), ignorando o ajuste global.

## 4. Pranchas iniciais (15 páginas, 317 botões)

`inicio` (Palavras principais, 29, com links para todas as páginas), `comida`, `sentimentos` (Como estou), `pessoas`, `atividades` (O que quero fazer), `lugares`, `corpo` (Meu corpo e cuidados), `frases` (Frases rápidas), `perguntas`, `escola`, `brincar` (Brincadeiras), `tempo` (Tempo e dias), `descricoes` (Cores e descrições), `nucleo` (Núcleo — 36 palavras essenciais, posições fixas, 6 colunas), `comecar` (Começar a falar — 18 inícios de frase pragmáticos).

Cores (chave de Fitzgerald adaptada): amarelo = pessoas/pronomes, verde = verbos, azul = descrições, laranja = coisas/lugares, roxo = perguntas, rosa = social/links, vermelho = urgente/negação.

Princípios do vocabulário (documentados em `docs/VOCABULARIO.md`): vocabulário núcleo (inspirado no *Universal Core* do Project Core, UNC), posições estáveis/planejamento motor (ideia pública do Gateway/LAMP), organização pragmática (ideia pública do PODD), cores de Fitzgerald. **Nada copiado de PCS/PODD/Gateway.**

## 5. Licenças — regras que NÃO podem ser quebradas

- **Código, docs e desenhos próprios:** MIT.
- **Pictogramas ARASAAC:** autor Sergio Palao, origem ARASAAC (arasaac.org), licença **CC BY-NC-SA 4.0**, propriedade do Governo de Aragão. **Não comercial** — compatível com a missão (app sempre grátis). A atribuição aparece no app (Sobre), READMEs, `THIRD_PARTY_NOTICES.md` e na provenance.
- **Mulberry Symbols:** Steve Lee, CC BY-SA 4.0.
- **Proibido:** incluir PCS, SymbolStix, PODD, Gateway, Boardmaker, vozes comerciais ou qualquer conteúdo proprietário. O dono pediu para "fazer REA/engenharia reversa" desses vocabulários: a resposta foi **não copiar**; em vez disso, criar vocabulário original seguindo princípios públicos. Mantenha essa linha.
- Arquivos de símbolos que já foram distribuídos não podem ser apagados (backups antigos apontam para eles): `scripts/shipped-symbols.json` lista esses nomes e o check exige que continuem existindo.

## 6. Build, testes e CI

### Build oficial (Windows, sem Gradle)
`node setup-tools.mjs` (baixa ferramentas com hash fixo em `tools.lock.json`: OpenJDK 17, AAPT2, D8/R8, apksig, android.jar API 35) → `./tests/run-model.ps1` → `python scripts/check-assets.py` → `./build.ps1` (AAPT2 + javac + D8 + `pack.py` + `Signer.java`). Saída: `build/Voz-Livre-<versão>.apk`. Detalhes em `BUILDING.md`. Existe `app/build.gradle` alternativo (AGP 8.7.3), não usado no release.

### Testes
- `tests/ModelTest.java` — 67 verificações na JVM (modelo, varredura, previsão, migrações, gramática, rotinas, i18n, pranchas EN/ES). Rodado por `tests/run-model.ps1` (fontes: Model, ScanEngine, Predictor, Grammar, I18n).
- `tests/RuntimeTest.java` — Robolectric 4.17, APIs 28 e 35: composição, ocultos, backup, varredura (inclusive **dentro de diálogos e por grupos**), idioma, formas das palavras, rotinas, importação TD Snap (usa a amostra pública `Medical Advocacy.spb` do AACProcessors, baixada com hash conferido).
- `tests/RenderTest.java` — renderiza telas (aparelho pt-BR simulado) → `build/inicio.png`, `comida.png`, `nucleo.png`, `paisagem.png`.
- `scripts/check-assets.py` — confere: 317 botões, tipos e destinos válidos, licenças e SHA-256 de cada símbolo, tabelas de migração, pranchas EN/ES sincronizadas (`build-boards.py --check`), **todo `t("...")` traduzido em en/es**, versões iguais em todos os arquivos, CHANGELOG com a versão, manifesto sem permissões.

### Workflows (`.github/workflows/`)
- `checks.yml` — em todo push/PR (Windows): check de ativos, ferramentas, testes de modelo, build com chave efêmera, dependências Robolectric, testes Robolectric. **Se os testes Robolectric falharem, a saída vai como anotação do GitHub** (dá para ler via API `check-runs/{job_id}/annotations`).
- `release.yml` — em push de tag `v*`: confere tag = versionName, roda checks, restaura a chave dos *secrets*, compila e assina, **recusa publicar se o certificado for diferente da release anterior**, publica `Voz-Livre-<versão>.apk`, `Voz-Livre.apk` e `SHA256SUMS.txt`.
- `screenshots.yml` — em branches `claude/**` quando o próprio arquivo muda (ou manual): renderiza e **commita** `docs/images/*.png`.
- `fetch-arasaac.yml` — em branches `claude/**` quando `scripts/arasaac-terms.json` muda (ou manual): roda `scripts/fetch-arasaac.py`, que baixa pictogramas ARASAAC (por ID e por busca em espanhol) e **commita** os PNG + `scripts/arasaac-meta.json`. Depois é preciso atualizar `default.json` e `symbols-provenance.json` (hashes) à mão/script.

### Assinatura (importante)
- A chave original está no PC do dono em `C:\Users\tiago\Documents\Codex\2026-10-15\https-github-com-morluto-rea-https\work\voz-livre\signing\` (`vozlivre.p12` + `password.txt`). **Nunca commitar.** Recomende ao dono guardar uma cópia fora do PC.
- Está cadastrada como *secrets* do repo: `SIGNING_KEY_BASE64` (p12 em base64) e `SIGNING_PASSWORD`. O release 0.2.0 e 0.3.0 confirmaram mesmo certificado da 0.1.1 → atualizações instalam por cima.

## 7. Como publicar uma versão (rotina que o dono já fez duas vezes)

1. Mudanças num branch → PR → CI verde.
2. Subir versão em **todos**: `AndroidManifest.xml` (versionCode/versionName), `app/build.gradle`, `package.json`, `BuildInfo.java`, e adicionar a seção `## x.y.z` no `CHANGELOG.md` (o check exige).
3. Dono faz **Merge pull request → Confirm merge**.
4. Dono cria a release no site: https://github.com/tiagocacote-bot/voz-livre/releases/new → **Tag: Select tag → Create new tag → `vX.Y.Z` → Create** → título `Voz Livre X.Y.Z` → descrição (texto do CHANGELOG + linha de download + créditos ARASAAC) → **Publish release** (não marcar pre-release, para o link "latest" funcionar).
5. A Action `Release APK` anexa os APKs em ~3 min. Conferir que `releases/latest/download/Voz-Livre.apk` baixa a versão nova.

## 8. Limitações do ambiente da IA anterior (Claude Code na nuvem) — para a próxima IA saber

- A IA conseguia: clonar, criar branches, dar push, abrir PR pela API REST, ler status de Actions e anotações.
- **Não conseguia:** fazer merge de PR (bloqueado por política "sem revisão"), criar tags/releases (push de tag e API de refs bloqueados), ler/escrever *secrets*, ler logs de Actions e baixar artefatos (domínios bloqueados), acessar `arasaac.org` e `dl.google.com` diretamente (por isso o download de pictogramas e o build rodam nas GitHub Actions).
- Compilação local de checagem era feita com `android-all` do Robolectric (Maven Central) como classpath; testes de modelo rodavam localmente; Robolectric só no CI.
- Cuidado com finais de linha: vários arquivos são **CRLF** (MainActivity.java, READMEs, CHANGELOG, build.ps1, docs/VALIDATION.md, RuntimeTest.java…) e outros **LF** (Model.java, SnapImporter.java, scripts). Preserve o original ao editar.

## 9. Publicação em lojas (em andamento)

- **Decisão do dono:** começar pelo caminho **grátis**. Escolhido: **Galaxy Store (Samsung)** — cadastro gratuito, e a loja é confiável nativamente em aparelhos Samsung (instala sem "fontes desconhecidas").
- Materiais prontos em `docs/store/galaxy/` (já na `main`): `LISTING.md` (passo a passo do portal https://seller.samsungapps.com, textos pt/en, categoria, classificação, respostas do formulário), `icon-512.png`, 4 capturas (simuladas — capturas reais do celular seriam melhores). Falta o dono: criar conta de vendedor, enviar, informar e-mail de suporte.
- **Play Store:** taxa única de US$ 25; contas pessoais novas exigem teste fechado com 12 testadores por 14 dias; exige **AAB** (o build atual só gera APK — precisaria adaptar, ex.: bundletool) e provavelmente target SDK mais novo que 35; usar a chave atual no Play App Signing para não quebrar atualizações de quem instalou pelo GitHub. Ainda não feito.
- **Outras grátis possíveis:** IzzyOnDroid (puxa APK das releases do GitHub), F-Droid (pode marcar "NonFreeAssets" pela licença NC do ARASAAC; exige build reprodutível), AppGallery, Aptoide.
- **Atenção:** o Google anunciou verificação obrigatória de desenvolvedores para instalar apps em aparelhos certificados, começando por Brasil e outros países em 2026 — isso pode afetar a instalação do APK do GitHub. Vale o dono verificar e se registrar.

## 10. O que ainda falta / ideias de próximos passos

Prioridade sugerida:
1. **Teste real** da 0.3.x no S24 FE e no tablet: atualização por cima, páginas novas, formas das palavras, troca de idioma, varredura com acionador (Bluetooth/USB), vozes EN/ES.
2. **Galaxy Store:** o dono ainda vai criar a conta de vendedor e enviar o app (materiais prontos em `docs/store/galaxy/`); depois acompanhar a revisão e responder exigências da Samsung.
3. **Play Store** (se o dono pagar): gerar AAB, subir target SDK, Data Safety, teste fechado.
4. Melhorias de produto possíveis:
   - Varredura por linha/coluna na tela principal (hoje é linear) e destaque/áudio de varredura configurável.
   - Busca de símbolos por palavras em inglês/espanhol (hoje as palavras-chave são em português).
   - Gramática para inglês/espanhol (hoje só pt).
   - Mais vocabulário revisado por fonoaudiólogos; páginas pessoais modelo (família, escola, terapia).
   - Calibração ocular por câmera (complexo; precisa de validação com usuários).
   - Exportar/compartilhar pranchas entre aparelhos; importar Open Board Format (OBF/OBZ, padrão aberto).
   - Tradução da interface para mais idiomas (adicionar `assets/i18n/<lang>.json` + `I18n.LANGUAGES`).
5. Validação com pessoas que usam CAA, famílias e profissionais — o app ainda é **alfa** e **sem validação clínica**; não prometer adequação clínica.

## 11. Preferências e jeito de trabalhar com o dono

- Ele quer que a IA "faça tudo sozinha" e vá postando no GitHub; ele dá as permissões que faltarem. Quando algo exigir ação dele (merge, release, secrets, lojas), explique em passos numerados, com o link exato e o nome exato dos botões.
- Ele valoriza: gratuidade total, acessibilidade para famílias pobres, respeito à comunidade autista (ex.: usar o infinito arco-íris, não a peça de quebra-cabeça), imagens ARASAAC do protótipo original.
- Seja honesto sobre o que foi ou não testado em aparelho real.

## 12. Links rápidos

- Repo: https://github.com/tiagocacote-bot/voz-livre
- Releases: https://github.com/tiagocacote-bot/voz-livre/releases
- Download direto: https://github.com/tiagocacote-bot/voz-livre/releases/latest/download/Voz-Livre.apk
- Nova release: https://github.com/tiagocacote-bot/voz-livre/releases/new
- Secrets: https://github.com/tiagocacote-bot/voz-livre/settings/secrets/actions
- Galaxy Store (vendedor): https://seller.samsungapps.com
- ARASAAC: https://arasaac.org · Mulberry: https://github.com/mulberrysymbols/mulberry-symbols
