# SerenityOS

> 🌐  [English](README.md) · [Türkçe](README.tr.md) · [中文 (简体)](README.zh-CN.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Русский](README.ru.md)

[![GitHub Actions Status](https://github.com/deimo-s/serenity/actions/workflows/ci.yml/badge.svg)](https://github.com/deimo-s/serenity/actions/workflows/ci.yml) [![Fuzzing Status](https://oss-fuzz-build-logs.storage.googleapis.com/badges/serenity.svg)](https://issues.oss-fuzz.com/issues?q=project:serenity) [![Discord](https://img.shields.io/discord/830522505605283862.svg?logo=discord&logoColor=white&logoWidth=20&labelColor=7289DA&label=Discord&color=17cf48)](https://serenityos.org/discord)

64 位 x86、Arm 和 RISC-V 计算机的图形化类 Unix 操作系统。

[常见问题](https://github.com/deimo-s/serenity/blob/master/Documentation/FAQ.md) | [文档](#如何阅读文档) | [构建说明](#如何构建和运行)

## 关于

SerenityOS 是一封写给 90 年代用户界面的情书，它拥有一个自定义的类 Unix 内核。它通过借鉴其他系统中优美的想法，真诚地取悦用户。

粗略地说，目标是 90 年代末生产力软件的美学与 00 年代末 \*nix 系统的强大用户可访问性的结合。这是一个由我们、为我们、基于我们喜爱的事物而构建的系统。

您可以在 YouTube 上观看该系统的开发视频：

- [Andreas Kling 的频道](https://youtube.com/andreaskling)
- [Linus Groh 的频道](https://youtube.com/linusgroh)
- [kleines Filmröllchen 的频道](https://www.youtube.com/c/kleinesfilmroellchen)

## 截图

![Screenshot c03b788.png](https://raw.githubusercontent.com/deimo-s/serenity/master/Meta/Screenshots/screenshot-c03b788.png)

## 特性

- 具有抢占式多线程的现代 64 位内核
- 带有 JavaScript、WebAssembly 等功能的[浏览器](https://github.com/deimo-s/serenity/blob/master/Userland/Applications/Browser)（查看 [JS](https://serenityos.github.io/libjs-website/test262/)、[CSS](https://css.tobyase.de/) 和 [Wasm](https://serenityos.github.io/libjs-website/wasm/) 的规范合规性）
- 安全特性（硬件保护、有限的用户空间能力、W^X 内存、`pledge` 和 `unveil`、(K)ASLR、OOM 抵抗、Web 内容隔离、最先进的 TLS 算法等）
- [系统服务](https://github.com/deimo-s/serenity/blob/master/Userland/Services)（WindowServer、LoginServer、AudioServer、WebServer、RequestServer、CrashServer 等）和现代 IPC
- 良好的 POSIX 兼容性（[LibC](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries/LibC)、Shell、系统调用、信号、伪终端、文件系统通知、标准的 Unix [实用程序](https://github.com/deimo-s/serenity/blob/master/Userland/Utilities) 等）
- 类 POSIX 的虚拟文件系统（/proc、/dev、/sys、/tmp 等）和 ext2 文件系统
- 支持 IPv4、TCP、UDP 的网络协议栈及应用程序，支持 DNS、HTTP、Gemini、IMAP、NTP
- 分析、调试和其他开发工具（内核支持的分析、CrashReporter、交互式 GUI playground、HexEditor、适用于 C++ 的 HackStudio IDE 等）
- 从密码学到 OpenGL、音频、JavaScript、GUI、下棋等各个领域的[库](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries)
- 支持许多常见和不常见的文件格式（PNG、JPEG、GIF、MP3、WAV、FLAC、ZIP、TAR、PDF、QOI、Gemini 等）
- 统一的样式和设计理念、灵活的主题系统、[自定义（位图和矢量）字体](https://fonts.serenityos.net/font-family)
- [游戏](https://github.com/deimo-s/serenity/blob/master/Userland/Games)（Solitaire、Minesweeper、2048、国际象棋、康威生命游戏等）和[演示程序](https://github.com/deimo-s/serenity/blob/master/Userland/Demos)（CatDog、Starfield、Eyes、曼德博集合、WidgetGallery 等）
- 日常 GUI 程序和实用工具（带 JavaScript 的电子表格、TextEditor、终端、PixelPaint、各种多媒体查看器和播放器、Mail、Assistant、计算器等）

…以上所有内容都包含在此仓库中，无需额外依赖，全部由我们从零开始构建 :^)

此外，还有[三百多个流行开源软件的移植版本](https://github.com/deimo-s/serenity/blob/master/Ports/AvailablePorts.md)，包括游戏、编译器、Unix 工具、多媒体应用程序等等。

## 如何阅读文档？

手册页可在 [man.serenityos.org](https://man.serenityos.org) 在线获取。这些页面是从 [`Base/usr/share/man`](https://github.com/SerenityOS/serenity/tree/master/Base/usr/shareman) 中的 Markdown 源文件生成的，并会自动更新。

运行 SerenityOS 时，您可以使用 `man` 命令访问终端界面，或使用 `help` 命令访问 GUI。

与代码相关的文档可在 [文档](https://github.com/deimo-s/serenity/blob/master/Documentation) 文件夹中找到。

## 如何构建和运行？

请参阅 [SerenityOS 构建说明](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructions.md) 或 [Ladybird 构建说明](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructionsLadybird.md)。

构建系统支持从 Linux、macOS、Windows（使用 WSL2）和许多其他 \*Nix 系统交叉编译 SerenityOS。默认的构建系统命令将启动一个 QEMU 实例来运行操作系统，根据支持情况启用硬件或软件虚拟化。

Ladybird 可以在与可作为 SerenityOS 交叉构建主机的相同平台上运行，也可以在 SerenityOS 本身运行。

## 联系我们并参与其中！

加入我们的 Discord 服务器：[SerenityOS Discord](https://serenityos.org/discord)

在提交问题之前，请参阅[问题策略](https://github.com/SerenityOS/serenity/blob/master/CONTRIBUTING.md#issue-policy)。

贡献指南可在 [`CONTRIBUTING.md`](https://github.com/deimo-s/serenity/blob/master/CONTRIBUTING.md) 中找到。

## 作者

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

以及更多贡献者！完整的贡献者列表请参见[这里](https://github.com/SerenityOS/serenity/graphs/contributors)。上面列出的人在项目中提交了 100 多个 commit。 :^)

## 许可证

SerenityOS 采用两条款 BSD 许可证授权。
