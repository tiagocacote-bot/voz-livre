# Changes

## 0.3.2 — falar ao tocar

- Em **Ajustes → Acesso**, novo botão "Adicionar à mensagem e falar ao mesmo tempo": ao tocar em uma palavra, ela entra na mensagem e é falada na hora. Útil em terapia e para reforço auditivo imediato. (É a mesma opção que já existia em **Ajustes → Voz e velocidade**, agora também no ajuste de toque.)
- *English:* in **Settings → Access**, a new "Add to the message and speak at the same time" option: tapping a word adds it to the message and speaks it immediately.

## 0.3.1 — novo ícone

- Novo ícone: um balão de fala (comunicação) com o símbolo do infinito nas cores do arco-íris (neurodiversidade), escolhido pela própria comunidade autista e neurodivergente — no lugar da peça de quebra-cabeça, que muitas pessoas autistas rejeitam. Ícone adaptativo: fica bem em telas com ícones redondos, quadrados ou arredondados.
- *English:* new adaptive icon — a speech bubble holding the rainbow infinity symbol of neurodiversity.

## 0.3.0 — varredura nas janelas, gramática, idiomas e mais vocabulário

- **Varredura em todas as janelas:** ajustes, edição, teclado, rotinas, listas e menus de escolha agora podem ser usados com acionador. Em janelas com muitas opções, a varredura passa primeiro por grupos de 8 e depois pelos itens do grupo escolhido. Perfil, Ajustes, Editar e "+ Novo botão" também entram na varredura, inclusive no modo de edição.
- **Rotinas com pictogramas:** cada etapa ganha uma imagem escolhida automaticamente pelo texto; toque na etapa para trocar. Duas rotinas iniciais: "Meu dia" e "Hora de dormir". Rotinas antigas continuam funcionando.
- **Gramática (pranchas em português):** toque em uma palavra da mensagem (ou **Mais → Mudar forma da última palavra**) para escolher outra forma: conjugação por sujeito e tempo (eu brinco, brinquei, brincava, brincarei), gênero e número (cansado, cansada, cansados). As sugestões já vêm conjugadas depois de "eu", "você", "nós"… A conjugação automática é opcional nos ajustes.
- **Idiomas:** interface em português, inglês e espanhol (automático pelo idioma do aparelho ou escolhido em **Ajustes → Idioma do aplicativo**). Um perfil novo pode começar com a prancha inicial em inglês ou espanhol, com voz e teclado do idioma.
- **Vocabulário aberto de núcleo e inícios de frase:** página **Núcleo** com 36 palavras essenciais em posições fixas (6 colunas) e página **Começar a falar** com inícios de frase por intenção ("Eu quero…", "Tenho uma pergunta", "Algo está errado"), que acrescentam o texto e abrem a página certa (novo tipo de botão "Adicionar e abrir página"). Vocabulário original, baseado em princípios públicos de CAA (vocabulário núcleo, planejamento motor, organização pragmática, cores de Fitzgerald) — sem copiar PCS, PODD ou Gateway. Veja `docs/VOCABULARIO.md`.
- **Mais vocabulário:** 5 páginas temáticas novas — Perguntas, Escola, Brincadeiras, Tempo e dias, Cores e descrições (317 botões em 15 páginas no total). Quem já usa o app recebe as páginas novas uma vez, sem mexer nas suas páginas. "Gostoso", "confortável" e "avó" ganharam pictogramas ARASAAC.
- **Importação do TD Snap melhorada:** mantém a ordem dos botões, as cores, os botões ocultos e importa as fotos da própria prancha. Símbolos de bibliotecas comerciais (como PCS) continuam sem ser copiados.

- *English:* scanning inside every dialog (with group scanning), visual routines with pictograms, Portuguese word forms and verb agreement, English and Spanish interface and starter boards, five new ARASAAC pages (261 buttons), and a better TD Snap import (order, colors, hidden buttons, page-set photos).

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
