# SerenityOS

> 🌐 **Languages / Diller:** [English](README.md) · [Türkçe](README.tr.md) · [中文 (简体)](README.zh-CN.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Русский](README.ru.md)

[![GitHub Actions Status](https://github.com/deimo-s/serenity/actions/workflows/ci.yml/badge.svg)](https://github.com/deimo-s/serenity/actions/workflows/ci.yml) [![Fuzzing Status](https://oss-fuzz-build-logs.storage.googleapis.com/badges/serenity.svg)](https://issues.oss-fuzz.com/issues?q=project:serenity) [![Discord](https://img.shields.io/discord/830522505605283862.svg?logo=discord&logoColor=white&logoWidth=20&labelColor=7289DA&label=Discord&color=17cf48)](https://serenityos.org/discord)

Système d'exploitation graphique de type Unix pour ordinateurs x86 64 bits, Arm et RISC-V.

[FAQ](https://github.com/deimo-s/serenity/blob/master/Documentation/FAQ.md) | [Documentation](#comment-lis-je-la-documentation) | [Instructions de compilation](#comment-compiler-et-exécuter-ce-projet)

## À propos

SerenityOS est une lettre d'amour aux interfaces utilisateur des années 90 avec un noyau de type Unix personnalisé. Il flatte avec sincérité en empruntant de belles idées à d'autres systèmes.

En gros, l'objectif est un mariage entre l'esthétique des logiciels de productivité de la fin des années 90 et l'accessibilité pour utilisateurs avancés du \*nix de la fin des années 2000. C'est un système par nous, pour nous, basé sur les choses que nous aimons.

Vous pouvez regarder des vidéos du développement du système sur YouTube :

- [Chaîne d'Andreas Kling](https://youtube.com/andreaskling)
- [Chaîne de Linus Groh](https://youtube.com/linusgroh)
- [Chaîne de kleines Filmröllchen](https://www.youtube.com/c/kleinesfilmroellchen)

## Capture d'écran

![Screenshot c03b788.png](https://raw.githubusercontent.com/deimo-s/serenity/master/Meta/Screenshots/screenshot-c03b788.png)

## Fonctionnalités

- Noyau 64 bits moderne avec multithreading préemptif
- [Navigateur](https://github.com/deimo-s/serenity/blob/master/Userland/Applications/Browser) avec JavaScript, WebAssembly et plus (vérifiez la conformité aux spécifications pour [JS](https://serenityos.github.io/libjs-website/test262/), [CSS](https://css.tobyase.de/) et [Wasm](https://serenityos.github.io/libjs-website/wasm/))
- Fonctionnalités de sécurité (protections matérielles, capacités limitées en espace utilisateur, mémoire W^X, `pledge` et `unveil`, (K)ASLR, résistance à l'OOM, isolation du contenu web, algorithmes TLS de pointe, ...)
- [Services système](https://github.com/deimo-s/serenity/blob/master/Userland/Services) (WindowServer, LoginServer, AudioServer, WebServer, RequestServer, CrashServer, ...) et IPC moderne
- Bonne compatibilité POSIX ([LibC](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries/LibC), Shell, appels système, signaux, pseudo-terminaux, notifications du système de fichiers, [utilitaires](https://github.com/deimo-s/serenity/blob/master/Userland/Utilities) Unix standard, ...)
- Systèmes de fichiers virtuels de type POSIX (/proc, /dev, /sys, /tmp, ...) et système de fichiers ext2
- Pile réseau et applications avec prise en charge d'IPv4, TCP, UDP ; DNS, HTTP, Gemini, IMAP, NTP
- Profilage, débogage et autres outils de développement (profilage supporté par le noyau, CrashReporter, terrain de jeu GUI interactif, HexEditor, IDE HackStudio pour C++ et plus)
- [Bibliothèques](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries) pour tout, de la cryptographie à OpenGL, audio, JavaScript, GUI, jouer aux échecs, ...
- Prise en charge de nombreux formats de fichiers courants et peu courants (PNG, JPEG, GIF, MP3, WAV, FLAC, ZIP, TAR, PDF, QOI, Gemini, ...)
- Philosophie de style et de conception unifiée, système de thèmes flexible, [polices personnalisées (bitmap et vectorielles)](https://fonts.serenityos.net/font-family)
- [Jeux](https://github.com/deimo-s/serenity/blob/master/Userland/Games) (Solitaire, Démineur, 2048, échecs, le Jeu de la vie de Conway, ...) et [démos](https://github.com/deimo-s/serenity/blob/master/Userland/Demos) (CatDog, Starfield, Eyes, ensemble de Mandelbrot, WidgetGallery, ...)
- Programmes GUI et utilitaires quotidiens (Tableur avec JavaScript, TextEditor, Terminal, PixelPaint, divers lecteurs multimédias, Mail, Assistant, Calculatrice, ...)

... et tout ce qui précède se trouve dans ce dépôt, sans dépendances supplémentaires, construit à partir de zéro par nous :^)

De plus, il y a [plus de trois cents ports de logiciels open source populaires](https://github.com/deimo-s/serenity/blob/master/Ports/AvailablePorts.md), incluant des jeux, compilateurs, outils Unix, applications multimédias et plus.

## Comment lis-je la documentation ?

Les pages de manuel sont disponibles en ligne sur [man.serenityos.org](https://man.serenityos.org). Ces pages sont générées à partir des fichiers source Markdown dans [`Base/usr/share/man`](https://github.com/SerenityOS/serenity/tree/master/Base/usr/shareman) et mises à jour automatiquement.

Lors de l'exécution de SerenityOS, vous pouvez utiliser `man` pour l'interface terminal, ou `help` pour la GUI.

La documentation relative au code se trouve dans le dossier [Documentation](https://github.com/deimo-s/serenity/blob/master/Documentation).

## Comment compiler et exécuter ce projet ?

Consultez les [instructions de compilation de SerenityOS](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructions.md) ou les [instructions de compilation de Ladybird](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructionsLadybird.md).

Le système de compilation prend en charge une compilation croisée de SerenityOS depuis Linux, macOS, Windows (avec WSL2) et de nombreux autres \*Nix. Les commandes par défaut du système de compilation lanceront une instance QEMU exécutant l'OS avec la virtualisation matérielle ou logicielle activée selon ce qui est supporté.

Ladybird s'exécute sur les mêmes plateformes qui peuvent être l'hôte d'une compilation croisée de SerenityOS et sur SerenityOS lui-même.

## Prenez contact et participez !

Rejoignez notre serveur Discord : [SerenityOS Discord](https://serenityos.org/discord)

Avant d'ouvrir un ticket, veuillez consulter la [politique des tickets](https://github.com/SerenityOS/serenity/blob/master/CONTRIBUTING.md#issue-policy).

Un guide général pour contribuer se trouve dans [`CONTRIBUTING.md`](https://github.com/deimo-s/serenity/blob/master/CONTRIBUTING.md).

## Auteurs

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

Et bien d'autres ! Voir [ici](https://github.com/SerenityOS/serenity/graphs/contributors) pour la liste complète des contributeurs. Les personnes listées ci-dessus ont mergé plus de 100 commits dans le projet. :^)

## Licence

SerenityOS est sous licence BSD à 2 clauses.
