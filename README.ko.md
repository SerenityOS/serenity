# SerenityOS

> 🌐  [English](README.md) · [Türkçe](README.tr.md) · [中文 (简体)](README.zh-CN.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Русский](README.ru.md)

[![GitHub Actions Status](https://github.com/deimo-s/serenity/actions/workflows/ci.yml/badge.svg)](https://github.com/deimo-s/serenity/actions/workflows/ci.yml) [![Fuzzing Status](https://oss-fuzz-build-logs.storage.googleapis.com/badges/serenity.svg)](https://issues.oss-fuzz.com/issues?q=project:serenity) [![Discord](https://img.shields.io/discord/830522505605283862.svg?logo=discord&logoColor=white&logoWidth=20&labelColor=7289DA&label=Discord&color=17cf48)](https://serenityos.org/discord)

64비트 x86, Arm 및 RISC-V 컴퓨터를 위한 그래픽 Unix 계열 운영 체제입니다.

[FAQ](https://github.com/deimo-s/serenity/blob/master/Documentation/FAQ.md) | [문서](#문서는-어디서-읽나요) | [빌드 안내](#빌드-및-실행-방법)

## 소개

SerenityOS는 90년대 사용자 인터페이스에 대한 러브레터이며, Unix 계열의 자체 커널을 갖추고 있습니다. 다양한 다른 시스템들에서 아름다운 아이디어들을 차용하여 진심으로 칭찬을 보냅니다.

대략적으로 말하면, 목표는 1990년대 후반 생산성 소프트웨어의 미학과 2000년대 후반 \*nix의 파워 유저 접근성을 결합하는 것입니다. 이것은 우리가, 우리를 위해, 우리가 좋아하는 것들에 기반한 시스템입니다.

YouTube에서 시스템 개발 과정을 보실 수 있습니다:

- [Andreas Kling의 채널](https://youtube.com/andreaskling)
- [Linus Groh의 채널](https://youtube.com/linusgroh)
- [kleines Filmröllchen의 채널](https://www.youtube.com/c/kleinesfilmroellchen)

## 스크린샷

![Screenshot c03b788.png](https://raw.githubusercontent.com/deimo-s/serenity/master/Meta/Screenshots/screenshot-c03b788.png)

## 기능

- 선제적 멀티스레딩을 지원하는 모던 64비트 커널
- JavaScript, WebAssembly 등을 지원하는 [브라우저](https://github.com/deimo-s/serenity/blob/master/Userland/Applications/Browser) ([JS](https://serenityos.github.io/libjs-website/test262/), [CSS](https://css.tobyase.de/), [Wasm](https://serenityos.github.io/libjs-website/wasm/) 명세 준수 여부를 확인하세요)
- 보안 기능 (하드웨어 보호, 제한된 유저랜드 권한, W^X 메모리, `pledge` 및 `unveil`, (K)ASLR, OOM 저항, 웹 콘텐츠 격리, 최첨단 TLS 알고리즘 등)
- [시스템 서비스](https://github.com/deimo-s/serenity/blob/master/Userland/Services) (WindowServer, LoginServer, AudioServer, WebServer, RequestServer, CrashServer 등) 및 모던 IPC
- 뛰어난 POSIX 호환성 ([LibC](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries/LibC), 셸, 시스템 콜, 시그널, 의사 터미널, 파일 시스템 알림, 표준 Unix [유틸리티](https://github.com/deimo-s/serenity/blob/master/Userland/Utilities) 등)
- POSIX 계열 가상 파일 시스템 (/proc, /dev, /sys, /tmp 등) 및 ext2 파일 시스템
- IPv4, TCP, UDP; DNS, HTTP, Gemini, IMAP, NTP를 지원하는 네트워크 스택 및 애플리케이션
- 프로파일링, 디버깅 및 기타 개발 도구 (커널 지원 프로파일링, CrashReporter, 인터랙티브 GUI 플레이그라운드, HexEditor, C++용 HackStudio IDE 등)
- 암호학부터 OpenGL, 오디오, JavaScript, GUI, 체스 플레이에 이르기까지 모든 것을 위한 [라이브러리](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries)
- 다양한 일반적/비일반적 파일 형식 지원 (PNG, JPEG, GIF, MP3, WAV, FLAC, ZIP, TAR, PDF, QOI, Gemini 등)
- 통일된 스타일 및 디자인 철학, 유연한 테마 시스템, [커스텀 (비트맵 및 벡터) 폰트](https://fonts.serenityos.net/font-family)
- [게임](https://github.com/deimo-s/serenity/blob/master/Userland/Games) (Solitaire, Minesweeper, 2048, 체스, Conway의 생명 게임 등) 및 [데모](https://github.com/deimo-s/serenity/blob/master/Userland/Demos) (CatDog, Starfield, Eyes, 만델브로 집합, WidgetGallery 등)
- 일상적인 GUI 프로그램 및 유틸리티 (JavaScript 지원 스프레드시트, TextEditor, 터미널, PixelPaint, 다양한 멀티미디어 뷰어 및 플레이어, Mail, Assistant, 계산기 등)

... 그리고 위의 모든 것이 이 저장소에 있으며, 추가 의존성 없이 우리 모두가 직접 만들었습니다 :^)

추가로, 게임, 컴파일러, Unix 도구, 멀티미디어 앱 등을 포함하는 [300개 이상의 인기 오픈소스 소프트웨어 포트](https://github.com/deimo-s/serenity/blob/master/Ports/AvailablePorts.md)가 있습니다.

## 문서는 어디서 읽나요?

맨페이지는 [man.serenityos.org](https://man.serenityos.org)에서 온라인으로 확인하실 수 있습니다. 이 페이지들은 [`Base/usr/share/man`](https://github.com/SerenityOS/serenity/tree/master/Base/usr/shareman)의 Markdown 소스 파일에서 생성되며 자동으로 업데이트됩니다.

SerenityOS를 실행 중일 때 터미널 인터페이스에는 `man`을, GUI에는 `help`를 사용하실 수 있습니다.

코드 관련 문서는 [Documentation](https://github.com/deimo-s/serenity/blob/master/Documentation) 폴더에서 찾으실 수 있습니다.

## 빌드 및 실행 방법

[SerenityOS 빌드 안내](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructions.md) 또는 [Ladybird 빌드 안내](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructionsLadybird.md)를 참조하세요.

빌드 시스템은 Linux, macOS, Windows (WSL2 사용) 및 기타 다양한 \*Nix에서의 SerenityOS 크로스 컴파일을 지원합니다. 기본 빌드 시스템 명령은 지원되는 하드웨어 또는 소프트웨어 가상화를 활성화한 상태로 OS를 실행하는 QEMU 인스턴스를 시작합니다.

Ladybird는 SerenityOS의 크로스 빌드 호스트가 될 수 있는 동일한 플랫폼과 SerenityOS 자체에서 실행됩니다.

## 연락하고 참여하세요!

Discord 서버에 참여하세요: [SerenityOS Discord](https://serenityos.org/discord)

이슈를 열기 전에 [이슈 정책](https://github.com/SerenityOS/serenity/blob/master/CONTRIBUTING.md#issue-policy)을 확인해 주세요.

기여에 대한 일반적인 안내는 [`CONTRIBUTING.md`](https://github.com/deimo-s/serenity/blob/master/CONTRIBUTING.md)에서 찾으실 수 있습니다.

## 작성자

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

그리고 더 많은 분들! 전체 기여자 목록은 [여기](https://github.com/SerenityOS/serenity/graphs/contributors)를 참조하세요. 위에 나열된 분들은 프로젝트에 100개 이상의 커밋을 기여하셨습니다. :^)

## 라이선스

SerenityOS는 2-clause BSD 라이선스 하에 라이선스가 부여됩니다.
