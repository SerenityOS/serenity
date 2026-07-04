# SerenityOS

> 🌐  [English](README.md) · [Türkçe](README.tr.md) · [中文 (简体)](README.zh-CN.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Русский](README.ru.md)

[![GitHub Actions Status](https://github.com/deimo-s/serenity/actions/workflows/ci.yml/badge.svg)](https://github.com/deimo-s/serenity/actions/workflows/ci.yml) [![Fuzzing Status](https://oss-fuzz-build-logs.storage.googleapis.com/badges/serenity.svg)](https://issues.oss-fuzz.com/issues?q=project:serenity) [![Discord](https://img.shields.io/discord/830522505605283862.svg?logo=discord&logoColor=white&logoWidth=20&labelColor=7289DA&label=Discord&color=17cf48)](https://serenityos.org/discord)

Sistema operativo tipo Unix con interfaz gráfica para ordenadores x86 de 64 bits, Arm y RISC-V.

[Preguntas Frecuentes](https://github.com/deimo-s/serenity/blob/master/Documentation/FAQ.md) | [Documentación](#cómo-leo-la-documentación) | [Instrucciones de compilación](#cómo-compilo-y-ejecuto-esto)

## Acerca de

SerenityOS es una carta de amor a las interfaces de usuario de los años 90 con un núcleo tipo Unix propio. Halaga con sinceridad al robar ideas hermosas de otros sistemas.

A grandes rasgos, el objetivo es un matrimonio entre la estética del software de productividad de finales de los 90 y la accesibilidad para usuarios avanzados del \*nix de finales de los 2000. Este es un sistema de nosotros, para nosotros, basado en las cosas que nos gustan.

Puedes ver videos del desarrollo del sistema en YouTube:

- [Canal de Andreas Kling](https://youtube.com/andreaskling)
- [Canal de Linus Groh](https://youtube.com/linusgroh)
- [Canal de kleines Filmröllchen](https://www.youtube.com/c/kleinesfilmroellchen)

## Captura de pantalla

![Screenshot c03b788.png](https://raw.githubusercontent.com/deimo-s/serenity/master/Meta/Screenshots/screenshot-c03b788.png)

## Características

- Núcleo moderno de 64 bits con multihilo preemptivo
- [Navegador](https://github.com/deimo-s/serenity/blob/master/Userland/Applications/Browser) con JavaScript, WebAssembly y más (consulta el cumplimiento de especificaciones para [JS](https://serenityos.github.io/libjs-website/test262/), [CSS](https://css.tobyase.de/) y [Wasm](https://serenityos.github.io/libjs-website/wasm/))
- Funciones de seguridad (protecciones de hardware, capacidades limitadas en el espacio de usuario, memoria W^X, `pledge` y `unveil`, (K)ASLR, resistencia a OOM, aislamiento de contenido web, algoritmos TLS de última generación, ...)
- [Servicios del sistema](https://github.com/deimo-s/serenity/blob/master/Userland/Services) (WindowServer, LoginServer, AudioServer, WebServer, RequestServer, CrashServer, ...) e IPC moderno
- Buena compatibilidad POSIX ([LibC](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries/LibC), Shell, syscalls, señales, pseudoterminales, notificaciones del sistema de archivos, [utilidades](https://github.com/deimo-s/serenity/blob/master/Userland/Utilities) estándar de Unix, ...)
- Sistemas de archivos virtuales tipo POSIX (/proc, /dev, /sys, /tmp, ...) y sistema de archivos ext2
- Pila de red y aplicaciones con soporte para IPv4, TCP, UDP; DNS, HTTP, Gemini, IMAP, NTP
- Herramientas de perfilado, depuración y otras de desarrollo (perfilado soportado por el núcleo, CrashReporter, playground GUI interactivo, HexEditor, HackStudio IDE para C++ y más)
- [Librerías](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries) para todo, desde criptografía hasta OpenGL, audio, JavaScript, GUI, jugar al ajedrez, ...
- Soporte para muchos formatos de archivo comunes y no comunes (PNG, JPEG, GIF, MP3, WAV, FLAC, ZIP, TAR, PDF, QOI, Gemini, ...)
- Filosofía de estilo y diseño unificada, sistema de temas flexible, [tipografías personalizadas (bitmap y vectoriales)](https://fonts.serenityos.net/font-family)
- [Juegos](https://github.com/deimo-s/serenity/blob/master/Userland/Games) (Solitaire, Minesweeper, 2048, ajedrez, El Juego de la Vida de Conway, ...) y [demos](https://github.com/deimo-s/serenity/blob/master/Userland/Demos) (CatDog, Starfield, Eyes, conjunto de Mandelbrot, WidgetGallery, ...)
- Programas GUI y utilidades de uso diario (Hoja de cálculo con JavaScript, TextEditor, Terminal, PixelPaint, varios visores y reproductores multimedia, Mail, Assistant, Calculadora, ...)

... y todo lo anterior está en este repositorio, sin dependencias extra, construido desde cero por nosotros :^)

Adicionalmente, hay [más de trescientos ports de software de código abierto popular](https://github.com/deimo-s/serenity/blob/master/Ports/AvailablePorts.md), incluyendo juegos, compiladores, herramientas Unix, aplicaciones multimedia y más.

## ¿Cómo leo la documentación?

Las páginas de manual están disponibles en línea en [man.serenityos.org](https://man.serenityos.org). Estas páginas se generan a partir de los archivos fuente Markdown en [`Base/usr/share/man`](https://github.com/SerenityOS/serenity/tree/master/Base/usr/shareman) y se actualizan automáticamente.

Cuando ejecutes SerenityOS puedes usar `man` para la interfaz de terminal, o `help` para la GUI.

La documentación relacionada con el código se puede encontrar en la carpeta [Documentation](https://github.com/deimo-s/serenity/blob/master/Documentation).

## ¿Cómo compilo y ejecuto esto?

Consulta las [instrucciones de compilación de SerenityOS](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructions.md) o las [instrucciones de compilación de Ladybird](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructionsLadybird.md).

El sistema de compilación admite una compilación cruzada de SerenityOS desde Linux, macOS, Windows (con WSL2) y muchos otros \*Nix. Los comandos por defecto del sistema de compilación lanzarán una instancia de QEMU ejecutando el sistema operativo con virtualización por hardware o software habilitada según sea compatible.

Ladybird se ejecuta en las mismas plataformas que pueden ser el host para una compilación cruzada de SerenityOS y en SerenityOS mismo.

## ¡Ponte en contacto y participa!

Únete a nuestro servidor de Discord: [SerenityOS Discord](https://serenityos.org/discord)

Antes de abrir un issue, por favor consulta la [política de issues](https://github.com/SerenityOS/serenity/blob/master/CONTRIBUTING.md#issue-policy).

Una guía general para contribuir se puede encontrar en [`CONTRIBUTING.md`](https://github.com/deimo-s/serenity/blob/master/CONTRIBUTING.md).

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

¡Y muchos más! Consulta [aquí](https://github.com/SerenityOS/serenity/graphs/contributors) la lista completa de contribuidores. Las personas listadas arriba han realizado más de 100 commits en el proyecto. :^)

## Licencia

SerenityOS está licenciado bajo una licencia BSD de 2 cláusulas.
