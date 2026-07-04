# SerenityOS

> 🌐 [English](README.md) · [Türkçe](README.tr.md) · [中文 (简体)](README.zh-CN.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Русский](README.ru.md)

[![GitHub Actions Status](https://github.com/deimo-s/serenity/actions/workflows/ci.yml/badge.svg)](https://github.com/deimo-s/serenity/actions/workflows/ci.yml) [![Fuzzing Status](https://oss-fuzz-build-logs.storage.googleapis.com/badges/serenity.svg)](https://issues.oss-fuzz.com/issues?q=project:serenity) [![Discord](https://img.shields.io/discord/830522505605283862.svg?logo=discord&logoColor=white&logoWidth=20&labelColor=7289DA&label=Discord&color=17cf48)](https://serenityos.org/discord)

Grafisches Unix-artiges Betriebssystem für 64-Bit-x86-, Arm- und RISC-V-Computer.

[FAQ](https://github.com/deimo-s/serenity/blob/master/Documentation/FAQ.md) | [Dokumentation](#wie-lese-ich-die-dokumentation) | [Build-Anleitung](#wie-baue-ich-dies-und-führe-es-aus)

## Über

SerenityOS ist ein Liebesbrief an die Benutzeroberflächen der 90er Jahre mit einem eigenen Unix-artigen Kernel. Es schmeichelt aufrichtig, indem es wunderschöne Ideen von anderen Systemen stiehlt.

Grob gesagt ist das Ziel eine Verbindung zwischen der Ästhetik von Produktivitätssoftware der späten 90er Jahre und der Power-User-Zugänglichkeit des späten 2000er \*nix. Dies ist ein System von uns, für uns, basierend auf den Dingen, die wir mögen.

Du kannst Videos von der Entwicklung des Systems auf YouTube ansehen:

- [Andreas Klings Kanal](https://youtube.com/andreaskling)
- [Linus Grohs Kanal](https://youtube.com/linusgroh)
- [kleines Filmröllchens Kanal](https://www.youtube.com/c/kleinesfilmroellchen)

## Screenshot

![Screenshot c03b788.png](https://raw.githubusercontent.com/deimo-s/serenity/master/Meta/Screenshots/screenshot-c03b788.png)

## Funktionen

- Moderner 64-Bit-Kernel mit präemptivem Multithreading
- [Browser](https://github.com/deimo-s/serenity/blob/master/Userland/Applications/Browser) mit JavaScript, WebAssembly und mehr (sieh dir die Spezifikationskonformität für [JS](https://serenityos.github.io/libjs-website/test262/), [CSS](https://css.tobyase.de/) und [Wasm](https://serenityos.github.io/libjs-website/wasm/) an)
- Sicherheitsfunktionen (Hardware-Schutz, eingeschränkte Userspace-Fähigkeiten, W^X-Speicher, `pledge` und `unveil`, (K)ASLR, OOM-Resistenz, Web-Content-Isolation, modernste TLS-Algorithmen, ...)
- [Systemdienste](https://github.com/deimo-s/serenity/blob/master/Userland/Services) (WindowServer, LoginServer, AudioServer, WebServer, RequestServer, CrashServer, ...) und moderne IPC
- Gute POSIX-Kompatibilität ([LibC](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries/LibC), Shell, Syscalls, Signale, Pseudoterminals, Dateisystembenachrichtigungen, Standard-Unix-[Hilfsprogramme](https://github.com/deimo-s/serenity/blob/master/Userland/Utilities), ...)
- POSIX-artige virtuelle Dateisysteme (/proc, /dev, /sys, /tmp, ...) und ext2-Dateisystem
- Netzwerkstack und Anwendungen mit Unterstützung für IPv4, TCP, UDP; DNS, HTTP, Gemini, IMAP, NTP
- Profiling, Debugging und andere Entwicklungswerkzeuge (kernelunterstütztes Profiling, CrashReporter, interaktiver GUI-Spielplatz, HexEditor, HackStudio-IDE für C++ und mehr)
- [Bibliotheken](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries) für alles, von Kryptographie bis OpenGL, Audio, JavaScript, GUI, Schach spielen, ...
- Unterstützung für viele gängige und ungewöhnliche Dateiformate (PNG, JPEG, GIF, MP3, WAV, FLAC, ZIP, TAR, PDF, QOI, Gemini, ...)
- Einheitliche Stil- und Designphilosophie, flexibles Themensystem, [benutzerdefinierte (Bitmap- und Vektor-)Schriften](https://fonts.serenityos.net/font-family)
- [Spiele](https://github.com/deimo-s/serenity/blob/master/Userland/Games) (Solitaire, Minesweeper, 2048, Schach, Conways Game of Life, ...) und [Demos](https://github.com/deimo-s/serenity/blob/master/Userland/Demos) (CatDog, Starfield, Eyes, Mandelbrot-Menge, WidgetGallery, ...)
- Alltägliche GUI-Programme und Hilfsprogramme (Tabellenkalkulation mit JavaScript, TextEditor, Terminal, PixelPaint, verschiedene Multimedia-Betrachter und -Player, Mail, Assistant, Taschenrechner, ...)

... und all das oben Genannte befindet sich in diesem Repository, ohne zusätzliche Abhängigkeiten, von Grund auf von uns gebaut :^)

Zusätzlich gibt es [über dreihundert Ports beliebter Open-Source-Software](https://github.com/deimo-s/serenity/blob/master/Ports/AvailablePorts.md), einschließlich Spielen, Compilern, Unix-Werkzeugen, Multimedia-Apps und mehr.

## Wie lese ich die Dokumentation?

Handbuchseiten sind online unter [man.serenityos.org](https://man.serenityos.org) verfügbar. Diese Seiten werden aus den Markdown-Quelldateien in [`Base/usr/share/man`](https://github.com/SerenityOS/serenity/tree/master/Base/usr/shareman) generiert und automatisch aktualisiert.

Beim Ausführen von SerenityOS kannst du `man` für die Terminal-Schnittstelle oder `help` für die GUI verwenden.

Code-bezogene Dokumentation findest du im [Documentation](https://github.com/deimo-s/serenity/blob/master/Documentation)-Ordner.

## Wie baue ich dies und führe es aus?

Sieh dir die [SerenityOS-Build-Anleitung](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructions.md) oder die [Ladybird-Build-Anleitung](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructionsLadybird.md) an.

Das Build-System unterstützt einen Cross-Compilation-Build von SerenityOS von Linux, macOS, Windows (mit WSL2) und vielen anderen \*Nixen aus. Die Standard-Build-System-Befehle starten eine QEMU-Instanz, die das Betriebssystem mit aktivierter Hardware- oder Software-Virtualisierung ausführt, sofern unterstützt.

Ladybird läuft auf denselben Plattformen, die als Host für einen Cross-Build von SerenityOS dienen können, und auf SerenityOS selbst.

## Tritt in Kontakt und mach mit!

Tritt unserem Discord-Server bei: [SerenityOS Discord](https://serenityos.org/discord)

Bevor du ein Issue öffnest, lies bitte die [Issue-Richtlinie](https://github.com/SerenityOS/serenity/blob/master/CONTRIBUTING.md#issue-policy).

Einen allgemeinen Leitfaden zum Mitwirken findest du in [`CONTRIBUTING.md`](https://github.com/deimo-s/serenity/blob/master/CONTRIBUTING.md).

## Autoren

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

Und viele weitere! Die vollständige Liste der Mitwirkenden findest du [hier](https://github.com/SerenityOS/serenity/graphs/contributors). Die oben aufgeführten Personen haben mehr als 100 Commits zum Projekt beigetragen. :^)

## Lizenz

SerenityOS ist unter einer 2-Klausel-BSD-Lizenz lizenziert.
