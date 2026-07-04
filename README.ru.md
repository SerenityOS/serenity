# SerenityOS

> 🌐  [English](README.md) · [Türkçe](README.tr.md) · [中文 (简体)](README.zh-CN.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Русский](README.ru.md)

[![GitHub Actions Status](https://github.com/deimo-s/serenity/actions/workflows/ci.yml/badge.svg)](https://github.com/deimo-s/serenity/actions/workflows/ci.yml) [![Fuzzing Status](https://oss-fuzz-build-logs.storage.googleapis.com/badges/serenity.svg)](https://issues.oss-fuzz.com/issues?q=project:serenity) [![Discord](https://img.shields.io/discord/830522505605283862.svg?logo=discord&logoColor=white&logoWidth=20&labelColor=7289DA&label=Discord&color=17cf48)](https://serenityos.org/discord)

Графическая Unix-подобная операционная система для 64-битных компьютеров на x86, Arm и RISC-V.

[FAQ](https://github.com/deimo-s/serenity/blob/master/Documentation/FAQ.md) | [Документация](#как-читать-документацию) | [Инструкции по сборке](#как-собрать-и-запустить)

## О проекте

SerenityOS — это письмо любви пользовательским интерфейсам 90-х с собственным Unix-подобным ядром. Оно льстит искренне, заимствуя красивые идеи из других систем.

Грубо говоря, цель — брак между эстетикой программ для продуктивности конца 90-х и доступностью для опытных пользователей \*nix конца 2000-х. Это система от нас, для нас, основанная на том, что нам нравится.

Вы можете смотреть видео разработки системы на YouTube:

- [Канал Andreas Kling](https://youtube.com/andreaskling)
- [Канал Linus Groh](https://youtube.com/linusgroh)
- [Канал kleines Filmröllchen](https://www.youtube.com/c/kleinesfilmroellchen)

## Скриншот

![Screenshot c03b788.png](https://raw.githubusercontent.com/deimo-s/serenity/master/Meta/Screenshots/screenshot-c03b788.png)

## Возможности

- Современное 64-битное ядро с вытесняющей многопоточностью
- [Браузер](https://github.com/deimo-s/serenity/blob/master/Userland/Applications/Browser) с JavaScript, WebAssembly и многим другим (проверьте соответствие спецификациям для [JS](https://serenityos.github.io/libjs-website/test262/), [CSS](https://css.tobyase.de/) и [Wasm](https://serenityos.github.io/libjs-website/wasm/))
- Функции безопасности (аппаратные защиты, ограниченные возможности пользовательского пространства, память W^X, `pledge` и `unveil`, (K)ASLR, устойчивость к OOM, изоляция веб-контента, современные алгоритмы TLS, ...)
- [Системные службы](https://github.com/deimo-s/serenity/blob/master/Userland/Services) (WindowServer, LoginServer, AudioServer, WebServer, RequestServer, CrashServer, ...) и современный IPC
- Хорошая совместимость с POSIX ([LibC](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries/LibC), Shell, системные вызовы, сигналы, псевдотерминалы, уведомления файловой системы, стандартные Unix-[утилиты](https://github.com/deimo-s/serenity/blob/master/Userland/Utilities), ...)
- POSIX-подобные виртуальные файловые системы (/proc, /dev, /sys, /tmp, ...) и файловая система ext2
- Сетевой стек и приложения с поддержкой IPv4, TCP, UDP; DNS, HTTP, Gemini, IMAP, NTP
- Профилирование, отладка и другие инструменты разработки (профилирование с поддержкой ядра, CrashReporter, интерактивная GUI-площадка, HexEditor, IDE HackStudio для C++ и другое)
- [Библиотеки](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries) для всего: от криптографии до OpenGL, аудио, JavaScript, GUI, игры в шахматы, ...
- Поддержка многих распространённых и редких форматов файлов (PNG, JPEG, GIF, MP3, WAV, FLAC, ZIP, TAR, PDF, QOI, Gemini, ...)
- Единая философия стиля и дизайна, гибкая система тем, [пользовательские (растровые и векторные) шрифты](https://fonts.serenityos.net/font-family)
- [Игры](https://github.com/deimo-s/serenity/blob/master/Userland/Games) (Solitaire, Minesweeper, 2048, шахматы, «Жизнь» Конвея, ...) и [демо](https://github.com/deimo-s/serenity/blob/master/Userland/Demos) (CatDog, Starfield, Eyes, множество Мандельброта, WidgetGallery, ...)
- Повседневные GUI-программы и утилиты (электронная таблица с JavaScript, TextEditor, Terminal, PixelPaint, различные мультимедийные просмотрщики и плееры, Mail, Assistant, Калькулятор, ...)

... и всё вышеперечисленное находится прямо в этом репозитории, без дополнительных зависимостей, создано нами с нуля :^)

Дополнительно, существует [более трёхсот портов популярного ПО с открытым исходным кодом](https://github.com/deimo-s/serenity/blob/master/Ports/AvailablePorts.md), включая игры, компиляторы, Unix-инструменты, мультимедийные приложения и многое другое.

## Как читать документацию?

Man-страницы доступны онлайн на [man.serenityos.org](https://man.serenityos.org). Эти страницы генерируются из Markdown-исходников в [`Base/usr/share/man`](https://github.com/SerenityOS/serenity/tree/master/Base/usr/shareman) и обновляются автоматически.

При работе в SerenityOS вы можете использовать `man` для терминального интерфейса или `help` для GUI.

Документация, связанная с кодом, находится в папке [Documentation](https://github.com/deimo-s/serenity/blob/master/Documentation).

## Как собрать и запустить?

Смотрите [инструкции по сборке SerenityOS](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructions.md) или [инструкции по сборке Ladybird](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructionsLadybird.md).

Система сборки поддерживает кросс-компиляцию SerenityOS из Linux, macOS, Windows (с WSL2) и многих других \*Nix. Команды системы сборки по умолчанию запустят экземпляр QEMU с ОС и включённой аппаратной или программной виртуализацией (если поддерживается).

Ladybird работает на тех же платформах, которые могут быть хостом для кросс-сборки SerenityOS, а также на самой SerenityOS.

## Связывайтесь и участвуйте!

Присоединяйтесь к нашему Discord-серверу: [SerenityOS Discord](https://serenityos.org/discord)

Прежде чем открыть issue, пожалуйста, ознакомьтесь с [политикой issue](https://github.com/SerenityOS/serenity/blob/master/CONTRIBUTING.md#issue-policy).

Общее руководство по участию можно найти в [`CONTRIBUTING.md`](https://github.com/deimo-s/serenity/blob/master/CONTRIBUTING.md).

## Авторы

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

И многие другие! Полный список участников смотрите [здесь](https://github.com/SerenityOS/serenity/graphs/contributors). Люди, перечисленные выше, внесли более 100 коммитов в проект. :^)

## Лицензия

SerenityOS лицензирован под лицензией BSD с 2 пунктами.
