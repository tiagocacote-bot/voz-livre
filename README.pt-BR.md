# Voz Livre — Free Voice

**Comunicação Aumentativa e Alternativa (CAA) gratuita e de código aberto para Android.**

[English](README.md) · [Baixar APK para Android](https://github.com/tiagocacote-bot/voz-livre/releases) · [Compilar o código](BUILDING.md)

A comunicação não deveria depender da renda ou da marca de um aparelho. O Voz Livre é um aplicativo gratuito de CAA para Android, criado para ampliar o acesso de pessoas autistas, outras pessoas neurodivergentes e qualquer pessoa que precise de apoio para se comunicar — especialmente famílias que não podem comprar um iPhone ou iPad e só têm aparelhos Android. Oferece um comunicador personalizável para celulares e tablets mais acessíveis. É gratuito para usar, estudar, adaptar e compartilhar, sem conta, assinatura ou anúncios.

**A versão 0.1.1 é uma alfa inicial.** O aplicativo e as pranchas estão em português brasileiro. O responsável pelo projeto relata que o uso básico aparentemente funcionou em um Samsung Galaxy S24 FE e em um tablet Samsung SM-P619. A versão do app, as versões do Android e os recursos testados não foram registrados; isso não confirma todos os métodos de acesso nem a nova edição 0.1.1 com outras imagens. Ainda precisamos ampliar a avaliação com usuários. É um projeto independente, sem vínculo com a Tobii Dynavox; não é o TD Snap e não oferece todos os recursos dele.

## Como experimentar

1. Baixe `Voz-Livre-0.1.1.apk` na [versão alfa](https://github.com/tiagocacote-bot/voz-livre/releases/tag/v0.1.1).
2. Abra em um Android 8.0 ou mais recente. Autorize a instalação pelo aplicativo usado para baixar o arquivo. Os recursos de acesso disponíveis dependem do aparelho e dos periféricos.
3. Nas configurações de voz do Android, baixe uma voz em português brasileiro para falar sem Internet. As pranchas e a edição são locais; a fala depende do motor de voz instalado.
4. Revise os símbolos, o vocabulário e os ajustes com a pessoa que vai utilizar. Exporte um backup em **Ajustes** antes de mudanças importantes.

## Recursos desta versão

- Oito páginas iniciais e 166 botões com imagem e texto para montar e falar mensagens.
- Edição de páginas, textos, fala, cores, links e visibilidade; fotos próprias pelo seletor do Android.
- Perfis locais separados, busca, teclado, notas, rotinas em texto, temporizador e desenho.
- Toque direto, tempo de permanência no toque, seleção ao soltar, pistas auditivas, permanência do ponteiro e varredura linear automática ou por passos. Precisam de testes com usuários e equipamentos.
- Backup e restauração em JSON com fotos, desfazer/refazer mensagens e desfazer edição de páginas.
- Importação experimental de `.spb`/`.sps`, com parte dos textos, links e comandos. Ações sem suporte ficam desativadas para revisão. Não preserva todas as imagens, posições ou ações avançadas.

## Limitações atuais

Não inclui calibração ocular, sincronização em nuvem, previsão gramatical, automação residencial, atividades Boardmaker nem vocabulários comerciais PCS/PODD/Gateway. A varredura não cobre todas as janelas e telas de edição. Switch Access, acionadores externos, ponteiros e motores de voz precisam de validação em aparelhos reais. Os testes automatizados não demonstram adequação clínica nem segurança para ser o único meio de comunicação de alguém.

O vocabulário e os símbolos são um ponto de partida editável. A versão 0.1.1 troca as imagens com restrição comercial do protótipo privado por Mulberry e desenhos próprios. **As imagens mudaram:** revise os botões conhecidos antes de migrar alguém que já usava o protótipo. Os nomes numéricos antigos continuam como aliases com as novas imagens.

![Voz Livre — Android](docs/images/paisagem.png)

Tela renderizada em simulação do Android API 35, não em aparelho físico.

## Privacidade e participação

Sem telemetria, anúncios, login ou servidor próprio. O aplicativo não pede permissão de Internet, microfone ou câmera. Os dados ficam no aparelho até você exportar ou compartilhar. O motor de voz e os destinos de compartilhamento possuem políticas próprias. Backups podem conter fotos e mensagens pessoais; guarde-os com cuidado. Veja [PRIVACY.md](PRIVACY.md).

Pessoas que utilizam CAA, autistas, famílias, profissionais de fonoaudiologia, tradutores e desenvolvedores são bem-vindos. Precisamos de testes em aparelhos acessíveis, relatos de barreiras, melhorias de vocabulário e traduções. Veja [CONTRIBUTING.md](CONTRIBUTING.md). Não publique dados pessoais ou backups de famílias nas issues.

## Licenças

Código, documentação e desenhos próprios: [MIT](LICENSE). **Mulberry Symbols by Steve Lee**: [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/), incluindo fontes SVG e conversões PNG. Preserve os créditos e as condições de compartilhamento das imagens derivadas. Consulte [os créditos completos](THIRD_PARTY_NOTICES.md) e a [origem de cada símbolo](app/src/main/assets/symbols-provenance.json).

Não inclui código proprietário do TD Snap, vozes ou símbolos comerciais PCS/PODD/Gateway. Pode ser redistribuído, inclusive comercialmente, respeitando as licenças aplicáveis.
