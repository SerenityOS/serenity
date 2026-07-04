# SerenityOS

> 🌐 **Languages / Diller:** [English](README.md) · [Türkçe](README.tr.md) · [中文 (简体)](README.zh-CN.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Русский](README.ru.md)

[![GitHub Actions Status](https://github.com/deimo-s/serenity/actions/workflows/ci.yml/badge.svg)](https://github.com/deimo-s/serenity/actions/workflows/ci.yml) [![Fuzzing Status](https://oss-fuzz-build-logs.storage.googleapis.com/badges/serenity.svg)](https://issues.oss-fuzz.com/issues?q=project:serenity) [![Discord](https://img.shields.io/discord/830522505605283862.svg?logo=discord&logoColor=white&logoWidth=20&labelColor=7289DA&label=Discord&color=17cf48)](https://serenityos.org/discord)

Sistema operacional gráfico do tipo Unix para computadores x86 de 64 bits, Arm e RISC-V.

[FAQ](https://github.com/deimo-s/serenity/blob/master/Documentation/FAQ.md) | [Documentação](#como-leio-a-documentação) | [Instruções de compilação](#como-compilo-e-exec-isto)

## Sobre

SerenityOS é uma carta de amor às interfaces de usuário dos anos 90 com um núcleo tipo Unix próprio. Elogia com sinceridade ao roubar belas ideias de outros sistemas.

Em termos gerais, o objetivo é um casamento entre a estética do software de produtividade do final dos anos 90 e a acessibilidade para usuários avançados do \*nix do final dos anos 2000. Este é um sistema nosso, para nós, baseado nas coisas que gostamos.

Você pode assistir a vídeos do desenvolvimento do sistema no YouTube:

- [Canal do Andreas Kling](https://youtube.com/andreaskling)
- [Canal do Linus Groh](https://youtube.com/linusgroh)
- [Canal do kleines Filmröllchen](https://www.youtube.com/c/kleinesfilmroellchen)

## Captura de tela

![Screenshot c03b788.png](https://raw.githubusercontent.com/deimo-s/serenity/master/Meta/Screenshots/screenshot-c03b788.png)

## Recursos

- Kernel moderno de 64 bits com multithreading preemptivo
- [Navegador](https://github.com/deimo-s/serenity/blob/master/Userland/Applications/Browser) com JavaScript, WebAssembly e mais (verifique a conformidade com as especificações para [JS](https://serenityos.github.io/libjs-website/test262/), [CSS](https://css.tobyase.de/) e [Wasm](https://serenityos.github.io/libjs-website/wasm/))
- Recursos de segurança (proteções de hardware, capacidades limitadas no espaço de usuário, memória W^X, `pledge` e `unveil`, (K)ASLR, resistência a OOM, isolamento de conteúdo web, algoritmos TLS de última geração, ...)
- [Serviços do sistema](https://github.com/deimo-s/serenity/blob/master/Userland/Services) (WindowServer, LoginServer, AudioServer, WebServer, RequestServer, CrashServer, ...) e IPC moderno
- Boa compatibilidade POSIX ([LibC](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries/LibC), Shell, syscalls, sinais, pseudoterminais, notificações do sistema de arquivos, [utilitários](https://github.com/deimo-s/serenity/blob/master/Userland/Utilities) Unix padrão, ...)
- Sistemas de arquivos virtuais no estilo POSIX (/proc, /dev, /sys, /tmp, ...) e sistema de arquivos ext2
- Pilha de rede e aplicações com suporte a IPv4, TCP, UDP; DNS, HTTP, Gemini, IMAP, NTP
- Ferramentas de criação de perfil, depuração e outras de desenvolvimento (criação de perfil com suporte do kernel, CrashReporter, playground GUI interativo, HexEditor, HackStudio IDE para C++ e mais)
- [Bibliotecas](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries) para tudo, desde criptografia até OpenGL, áudio, JavaScript, GUI, jogar xadrez, ...
- Suporte para muitos formatos de arquivo comuns e incomuns (PNG, JPEG, GIF, MP3, WAV, FLAC, ZIP, TAR, PDF, QOI, Gemini, ...)
- Filosofia unificada de estilo e design, sistema de temas flexível, [fontes personalizadas (bitmap e vetoriais)](https://fonts.serenityos.net/font-family)
- [Jogos](https://github.com/deimo-s/serenity/blob/master/Userland/Games) (Solitaire, Minesweeper, 2048, xadrez, Jogo da Vida de Conway, ...) e [demos](https://github.com/deimo-s/serenity/blob/master/Userland/Demos) (CatDog, Starfield, Eyes, conjunto de Mandelbrot, WidgetGallery, ...)
- Programas GUI e utilitários do dia a dia (Planilha com JavaScript, TextEditor, Terminal, PixelPaint, vários visualizadores e reprodutores multimídia, Mail, Assistant, Calculadora, ...)

... e tudo o que está acima está neste repositório, sem dependências extras, construído do zero por nós :^)

Adicionalmente, há [mais de trezentos ports de software de código aberto popular](https://github.com/deimo-s/serenity/blob/master/Ports/AvailablePorts.md), incluindo jogos, compiladores, ferramentas Unix, aplicativos multimídia e mais.

## Como leio a documentação?

As páginas de manual estão disponíveis online em [man.serenityos.org](https://man.serenityos.org). Essas páginas são geradas a partir dos arquivos-fonte Markdown em [`Base/usr/share/man`](https://github.com/SerenityOS/serenity/tree/master/Base/usr/shareman) e atualizadas automaticamente.

Ao executar o SerenityOS você pode usar `man` para a interface de terminal, ou `help` para a GUI.

A documentação relacionada ao código pode ser encontrada na pasta [Documentation](https://github.com/deimo-s/serenity/blob/master/Documentation).

## Como compilo e executo isto?

Veja as [instruções de compilação do SerenityOS](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructions.md) ou as [instruções de compilação do Ladybird](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructionsLadybird.md).

O sistema de compilação suporta uma compilação cruzada do SerenityOS a partir de Linux, macOS, Windows (com WSL2) e muitos outros \*Nix. Os comandos padrão do sistema de compilação iniciarão uma instância do QEMU executando o SO com virtualização por hardware ou software habilitada conforme suportado.

O Ladybird roda nas mesmas plataformas que podem ser o host para uma compilação cruzada do SerenityOS e no próprio SerenityOS.

## Entre em contato e participe!

Junte-se ao nosso servidor do Discord: [SerenityOS Discord](https://serenityos.org/discord)

Antes de abrir uma issue, por favor veja a [política de issues](https://github.com/SerenityOS/serenity/blob/master/CONTRIBUTING.md#issue-policy).

Um guia geral para contribuir pode ser encontrado em [`CONTRIBUTING.md`](https://github.com/deimo-s/serenity/blob/master/CONTRIBUTING.md).

## Autores

- **Andreas Kling** - [awesomekling](https://twitter.com/awesomekling) [![GitHub Sponsors](https://img.shields.io/static/v1?label=Sponsor&message=%E2%9D%A4&logo=GitHub)](https://github.com/sponsors/awesomekling)
- **Robin Burchell** - [rburchell](https://github.com/rburchell)
- **Conrad Pankoff** - [deoxxa](https://github.com/deoxxa)
- **Sergey Bugaev** - [bugaevc](https://github.com/bugaevc)
- **Liav A** - [supercomputer7](https://github.com/supercomputer7)
- **Linus Groh** - [linusg](https://github.com/linusg) [![GitHub Sponsors](https://img.shields.io/static/v1?label=Sponsor&message=%E2%9D%A4&logo=GitHub)](https://github.com/sponsors/linusg)
- **Ali Mohammad Pur** - [alimpfard](https://github.com/alimpfard)
- **Shannon Booth** - [shannonbooth](https://github.com/shannonbooth)
- **Hüseyin ASLITÜRK** - [asliturk](https://github.com/asliturk)
- **Matthew Olsson** - [mattco98](https://github.com/mattco98)
- **Nico Weber** - [nico](https://github.com/nico)
- **Brian Gianforcaro** - [bgianfo](https://github.com/bgianfo)
- **Ben Wiederhake** - [BenWiederhake](https://github.com/BenWiederhake)
- **Tom** - [tomuta](https://github.com/tomuta)
- **Paul Scharnofske** - [asynts](https://github.com/asynts)
- **Itamar Shenhar** - [itamar8910](https://github.com/itamar8910)
- **Luke Wilde** - [Lubrsi](https://github.com/Lubrsi)
- **Brendan Coles** - [bcoles](https://github.com/bcoles)
- **Andrew Kaster** - [ADKaster](https://github.com/ADKaster) [![GitHub Sponsors](https://img.shields.io/static/v1?label=Sponsor&message=%E2%9D%A4&logo=GitHub)](https://github.com/sponsors/ADKaster)
- **thankyouverycool** - [thankyouverycool](https://github.com/thankyouverycool)
- **Idan Horowitz** - [IdanHo](https://github.com/IdanHo)
- **Gunnar Beutner** - [gunnarbeutner](https://github.com/gunnarbeutner)
- **Tim Flynn** - [trflynn89](https://github.com/trflynn89)
- **Jean-Baptiste Boric** - [boricj](https://github.com/boricj)
- **Stephan Unverwerth** - [sunverwerth](https://github.com/sunverwerth)
- **Max Wipfli** - [MaxWipfli](https://github.com/MaxWipfli)
- **Daniel Bertalan** - [BertalanD](https://github.com/BertalanD)
- **Jelle Raaijmakers** - [GMTA](https://github.com/GMTA)
- **Sam Atkins** - [AtkinsSJ](https://github.com/AtkinsSJ) [![GitHub Sponsors](https://img.shields.io/static/v1?label=Sponsor&message=%E2%9D%A4&logo=GitHub)](https://github.com/sponsors/AtkinsSJ)
- **Tobias Christiansen** - [TobyAsE](https://github.com/TobyAsE)
- **Lenny Maiorani** - [ldm5180](https://github.com/ldm5180)
- **sin-ack** - [sin-ack](https://github.com/sin-ack)
- **Jesse Buhagiar** - [Quaker762](https://github.com/Quaker762)
- **Peter Elliott** - [Petelliott](https://github.com/Petelliott)
- **Karol Kosek** - [krkk](https://github.com/krkk)
- **Mustafa Quraish** - [mustafaquraish](https://github.com/mustafaquraish)
- **David Tuin** - [davidot](https://github.com/davidot)
- **Leon Albrecht** - [Hendiadyoin1](https://github.com/Hendiadyoin1)
- **Tim Schumacher** - [timschumi](https://github.com/timschumi)
- **Marcus Nilsson** - [metmo](https://github.com/metmo)
- **Gegga Thor** - [Xexxa](https://github.com/Xexxa) [![GitHub Sponsors](https://img.shields.io/static/v1?label=Sponsor&message=%E2%9D%A4&logo=GitHub)](https://github.com/sponsors/Xexxa)
- **kleines Filmröllchen** - [kleinesfilmroellchen](https://github.com/kleinesfilmroellchen) [![GitHub Sponsors](https://img.shields.io/static/v1?label=Sponsor&message=%E2%9D%A4&logo=GitHub)](https://github.com/sponsors/kleinesfilmroellchen)
- **Kenneth Myhra** - [kennethmyhra](https://github.com/kennethmyhra)
- **Maciej** - [sppmacd](https://github.com/sppmacd)
- **Sahan Fernando** - [ccapitalK](https://github.com/ccapitalK)
- **Benjamin Maxwell** - [MacDue](https://github.com/MacDue)
- **Dennis Esternon** - [djwisdom](https://github.com/djwisdom) [![GitHub Sponsors](https://img.shields.io/static/v1?label=Sponsor&message=%E2%9D%A4&logo=GitHub)](https://github.com/sponsors/djwisdom)
- **frhun** - [frhun](https://github.com/frhun)
- **networkException** - [networkException](https://github.com/networkException) [![GitHub Sponsors](https://img.shields.io/static/v1?label=Sponsor&message=%E2%9D%A4&logo=GitHub)](https://github.com/sponsors/networkException)
- **Brandon Jordan** - [electrikmilk](https://github.com/electrikmilk)
- **Lucas Chollet** - [LucasChollet](https://github.com/LucasChollet)
- **Timon Kruiper** - [FireFox317](https://github.com/FireFox317)
- **Martin Falisse** - [martinfalisse](https://github.com/martinfalisse)
- **Gregory Bertilson** - [Zaggy1024](https://github.com/Zaggy1024)
- **Erik Wouters** - [EWouters](https://github.com/EWouters)
- **Rodrigo Tobar** - [rtobar](https://github.com/rtobar)
- **Alexander Kalenik** - [kalenikaliaksandr](https://github.com/kalenikaliaksandr)
- **Tim Ledbetter** - [tcl3](https://github.com/tcl3)
- **Steffen T. Larssen** - [stelar7](https://github.com/stelar7)
- **Andi Gallo** - [axgallo](https://github.com/axgallo)
- **Simon Wanner** - [skyrising](https://github.com/skyrising)
- **FalseHonesty** - [FalseHonesty](https://github.com/FalseHonesty)
- **Bastiaan van der Plaat** - [bplaat](https://github.com/bplaat)
- **Dan Klishch** - [DanShaders](https://github.com/DanShaders)
- **Julian Offenhäuser** - [janso3](https://github.com/janso3)
- **Sönke Holz** - [spholz](https://github.com/spholz)
- **implicitfield** - [implicitfield](https://github.com/implicitfield)

E muitos mais! Veja [aqui](https://github.com/SerenityOS/serenity/graphs/contributors) a lista completa de contribuidores. As pessoas listadas acima realizaram mais de 100 commits no projeto. :^)

## Licença

SerenityOS é licenciado sob uma licença BSD de 2 cláusulas.
