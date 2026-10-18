# Changes

## 0.2.0 — pictogramas ARASAAC e previsão de palavras

- Os botões iniciais voltam a usar os pictogramas ARASAAC do protótipo original (163 de 166 botões). Quem atualiza da 0.1.1 tem os botões iniciais trocados automaticamente; fotos pessoais e símbolos escolhidos à mão são mantidos. Em **Ajustes → Tamanho da grade e letras** é possível voltar para Mulberry e desenhos próprios.
- Previsão de palavras: uma linha com 4 sugestões abaixo dos controles de fala, também no teclado. Começa com sugestões iniciais em português e aprende, só no aparelho, com as mensagens faladas de cada perfil. Pode ser desligada ou apagada nos ajustes.
- A varredura pula sugestões vazias e linhas ocultas.
- Releases publicadas pela GitHub Action com a mesma chave da 0.1.1 (atualiza sem desinstalar) e link fixo para a versão mais recente: `releases/latest/download/Voz-Livre.apk`.
- Licença das imagens: os pictogramas ARASAAC são CC BY-NC-SA 4.0 (uso não comercial). O código continua MIT.

- *English:* starter buttons use the original prototype's ARASAAC pictograms again (CC BY-NC-SA 4.0, non-commercial), with automatic migration from 0.1.1 and an option to switch back to Mulberry; local next-word prediction; signed CI releases with a stable latest-APK link.

## 0.1.1 — public alpha

- First public open-source Android edition, with English and Brazilian Portuguese documentation.
- Replace private prototype artwork with Mulberry Symbols (CC BY-SA 4.0) and original MIT drawings. Include editable SVGs and per-symbol provenance. Existing numeric aliases contain the new images.
- Preserve prototype functionality, with alpha limitations documented. UI and starter vocabulary remain Brazilian Portuguese.
- Android 8+; compiled against/targeting API 35. Basic operation reported by the project owner on a Galaxy S24 FE and a Samsung SM-P619 tablet; build and feature coverage unconfirmed. No clinical validation.

Private prototype 0.1.0 was not a public release. The public code excludes its non-commercial artwork and private signing material.
