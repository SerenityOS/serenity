# SerenityOS

> 🌐  [English](README.md) · [Türkçe](README.tr.md) · [中文 (简体)](README.zh-CN.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Русский](README.ru.md)

[![GitHub Actions Status](https://github.com/deimo-s/serenity/actions/workflows/ci.yml/badge.svg)](https://github.com/deimo-s/serenity/actions/workflows/ci.yml) [![Fuzzing Status](https://oss-fuzz-build-logs.storage.googleapis.com/badges/serenity.svg)](https://issues.oss-fuzz.com/issues?q=project:serenity) [![Discord](https://img.shields.io/discord/830522505605283862.svg?logo=discord&logoColor=white&logoWidth=20&labelColor=7289DA&label=Discord&color=17cf48)](https://serenityos.org/discord)

64 ビット x86、Arm、RISC-V コンピューター向けのグラフィカルな Unix 系オペレーティングシステムです。

[FAQ](https://github.com/deimo-s/serenity/blob/master/Documentation/FAQ.md) | [ドキュメント](#ドキュメントの読み方) | [ビルド手順](#ビルドと実行方法)

## 概要

SerenityOS は、90 年代のユーザーインターフェースへのラブレターであり、Unix 系カスタムコアを備えています。さまざまな他のシステムから美しいアイデアを借りて、心からの敬意を表しています。

大雑把に言えば、目標は 1990 年代後半の生産性ソフトウェアの美学と、2000 年代後半の \*nix システムのパワーユーザー向けアクセシビリティを結びつけることです。これは、私たちによる、私たちのための、私たちが好きなものに基づいたシステムです。

YouTube でこのシステムの開発動画を視聴できます：

- [Andreas Kling のチャンネル](https://youtube.com/andreaskling)
- [Linus Groh のチャンネル](https://youtube.com/linusgroh)
- [kleines Filmröllchen のチャンネル](https://www.youtube.com/c/kleinesfilmroellchen)

## スクリーンショット

![Screenshot c03b788.png](https://raw.githubusercontent.com/deimo-s/serenity/master/Meta/Screenshots/screenshot-c03b788.png)

## 機能

- プリエンプティブなマルチスレッドを備えたモダンな 64 ビットカーネル
- JavaScript、WebAssembly などを搭載した[ブラウザ](https://github.com/deimo-s/serenity/blob/master/Userland/Applications/Browser)（[JS](https://serenityos.github.io/libjs-website/test262/)、[CSS](https://css.tobyase.de/)、[Wasm](https://serenityos.github.io/libjs-website/wasm/) の仕様準拠を確認してください）
- セキュリティ機能（ハードウェア保護、限定されたユーザーランド機能、W^X メモリ、`pledge` と `unveil`、(K)ASLR、OOM 耐性、Web コンテンツ分離、最先端の TLS アルゴリズムなど）
- [システムサービス](https://github.com/deimo-s/serenity/blob/master/Userland/Services)（WindowServer、LoginServer、AudioServer、WebServer、RequestServer、CrashServer など）とモダンな IPC
- 優れた POSIX 互換性（[LibC](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries/LibC)、シェル、システムコール、シグナル、擬似端末、ファイルシステム通知、標準 Unix [ユーティリティ](https://github.com/deimo-s/serenity/blob/master/Userland/Utilities) など）
- POSIX 風の仮想ファイルシステム（/proc、/dev、/sys、/tmp など）と ext2 ファイルシステム
- IPv4、TCP、UDP をサポートするネットワークスタックとアプリケーション。DNS、HTTP、Gemini、IMAP、NTP に対応
- プロファイリング、デバッグ、その他の開発ツール（カーネル対応プロファイリング、CrashReporter、インタラクティブな GUI プレイグラウンド、HexEditor、C++ 用の HackStudio IDE など）
- 暗号化から OpenGL、オーディオ、JavaScript、GUI、チェスプレイまであらゆる[ライブラリ](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries)
- 多くの一般的・非一般的なファイル形式をサポート（PNG、JPEG、GIF、MP3、WAV、FLAC、ZIP、TAR、PDF、QOI、Gemini など）
- 統一されたスタイルとデザイン哲学、柔軟なテーマシステム、[カスタム（ビットマップとベクター）フォント](https://fonts.serenityos.net/font-family)
- [ゲーム](https://github.com/deimo-s/serenity/blob/master/Userland/Games)（Solitaire、Minesweeper、2048、チェス、コンウェイのライフゲームなど）と[デモ](https://github.com/deimo-s/serenity/blob/master/Userland/Demos)（CatDog、Starfield、Eyes、マンデルブロ集合、WidgetGallery など）
- 日常的な GUI プログラムとユーティリティ（JavaScript 搭載のスプレッドシート、TextEditor、ターミナル、PixelPaint、各種マルチメディアビューア・プレーヤー、メール、アシスタント、電卓など）

…上記のすべてがこのリポジトリにあり、追加の依存関係はなく、私たちがゼロから構築しました :^)

さらに、ゲーム、コンパイラ、Unix ツール、マルチメディアアプリなどを含む[300 以上の人気オープンソースソフトウェアの移植](https://github.com/deimo-s/serenity/blob/master/Ports/AvailablePorts.md)があります。

## ドキュメントの読み方

man ページは [man.serenityos.org](https://man.serenityos.org) でオンライン閲覧できます。これらのページは [`Base/usr/share/man`](https://github.com/SerenityOS/serenity/tree/master/Base/usr/shareman) にある Markdown ソースファイルから生成され、自動的に更新されます。

SerenityOS を実行しているときは、ターミナルインターフェース用に `man` を、GUI 用に `help` を使用できます。

コード関連のドキュメントは [Documentation](https://github.com/deimo-s/serenity/blob/master/Documentation) フォルダにあります。

## ビルドと実行方法

[SerenityOS のビルド手順](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructions.md) または [Ladybird のビルド手順](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructionsLadybird.md) を参照してください。

ビルドシステムは、Linux、macOS、Windows（WSL2 経由）、その他多くの \*Nix からの SerenityOS のクロスコンパイルビルドをサポートしています。デフォルトのビルドシステムコマンドは、サポートされているハードウェアまたはソフトウェア仮想化を有効にして、OS を実行する QEMU インスタンスを起動します。

Ladybird は、SerenityOS のクロスビルドのホストになりうる同じプラットフォーム、および SerenityOS 自体で動作します。

## 参加して交流しましょう！

Discord サーバーへ参加：[SerenityOS Discord](https://serenityos.org/discord)

Issue を開く前に、[Issue ポリシー](https://github.com/SerenityOS/serenity/blob/master/CONTRIBUTING.md#issue-policy) を確認してください。

貢献に関する一般的なガイドは [`CONTRIBUTING.md`](https://github.com/deimo-s/serenity/blob/master/CONTRIBUTING.md) にあります。

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

その他多数！完全な貢献者リストは[こちら](https://github.com/SerenityOS/serenity/graphs/contributors)をご覧ください。上記にリストされている人々は、このプロジェクトに 100 以上のコミットを貢献しています。 :^)

## ライセンス

SerenityOS は 2 条項 BSD ライセンスの下でライセンスされています。
