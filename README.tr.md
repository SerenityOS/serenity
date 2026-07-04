# SerenityOS

> 🌐  [English](README.md) · [Türkçe](README.tr.md) · [中文 (简体)](README.zh-CN.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Русский](README.ru.md)

[![GitHub Actions Status](https://github.com/deimo-s/serenity/actions/workflows/ci.yml/badge.svg)](https://github.com/deimo-s/serenity/actions/workflows/ci.yml) [![Fuzzing Status](https://oss-fuzz-build-logs.storage.googleapis.com/badges/serenity.svg)](https://issues.oss-fuzz.com/issues?q=project:serenity) [![Discord](https://img.shields.io/discord/830522505605283862.svg?logo=discord&logoColor=white&logoWidth=20&labelColor=7289DA&label=Discord&color=17cf48)](https://serenityos.org/discord)

64-bit x86, Arm ve RISC-V bilgisayarlar için grafiksel Unix-benzeri işletim sistemi.

[Sıkça Sorulan Sorular](https://github.com/deimo-s/serenity/blob/master/Documentation/FAQ.md) | [Belgeler](#belgeleri-nasıl-okurum) | [Derleme Talimatları](#derleyip-çalıştırmak)

## Hakkında

SerenityOS, '90'ların kullanıcı arayüzlerine yazılmış bir aşk mektubudur ve özel bir Unix-benzeri çekirdeğe sahiptir. Çeşitli diğer sistemlerden güzel fikirleri ödünç alarak içtenlikle pohpohlar.

Kabaca söylemek gerekirse, hedef 1990'ların sonlarındaki üretkenlik yazılımlarının estetiği ile 2000'lerin sonlarındaki \*nix sistemlerinin güçlü kullanıcı erişilebilirliğinin evliliğidir. Bu, bizim için, bizim tarafımızdan, sevdiğimiz şeylere dayalı bir sistemdir.

Sistemin geliştirilmesini YouTube'da izleyebilirsiniz:

- [Andreas Kling'in kanalı](https://youtube.com/andreaskling)
- [Linus Groh'un kanalı](https://youtube.com/linusgroh)
- [kleines Filmröllchen'in kanalı](https://www.youtube.com/c/kleinesfilmroellchen)

## Ekran Görüntüsü

![Ekran Görüntüsü c03b788.png](https://raw.githubusercontent.com/deimo-s/serenity/master/Meta/Screenshots/screenshot-c03b788.png)

## Özellikler

- Önleyici çoklu iş parçacığı desteği ile modern 64-bit çekirdek
- JavaScript, WebAssembly ve daha fazlasıyla [Tarayıcı](https://github.com/deimo-s/serenity/blob/master/Userland/Applications/Browser) ([JS](https://serenityos.github.io/libjs-website/test262/), [CSS](https://css.tobyase.de/) ve [Wasm](https://serenityos.github.io/libjs-website/wasm/) uyumluluğunu kontrol edin)
- Güvenlik özellikleri (donanım korumaları, sınırlı kullanıcı alanı yetenekleri, W^X bellek, `pledge` ve `unveil`, (K)ASLR, OOM direnci, web içeriği izolasyonu, son teknoloji TLS algoritmaları, ...)
- [Sistem servisleri](https://github.com/deimo-s/serenity/blob/master/Userland/Services) (WindowServer, LoginServer, AudioServer, WebServer, RequestServer, CrashServer, ...) ve modern IPC
- İyi POSIX uyumluluğu ([LibC](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries/LibC), Shell, sistem çağrıları, sinyaller, sözde terminaller, dosya sistemi bildirimleri, standart Unix [yardımcı programları](https://github.com/deimo-s/serenity/blob/master/Userland/Utilities), ...)
- POSIX-benzeri sanal dosya sistemleri (/proc, /dev, /sys, /tmp, ...) ve ext2 dosya sistemi
- IPv4, TCP, UDP; DNS, HTTP, Gemini, IMAP, NTP destekli ağ yığını ve uygulamaları
- Profil oluşturma, hata ayıklama ve diğer geliştirme araçları (Çekirdek destekli profil oluşturma, CrashReporter, etkileşimli GUI oyun alanı, HexEditor, C++ için HackStudio IDE ve daha fazlası)
- Kriptografiden OpenGL'e, ses, JavaScript, GUI, satranç oynamaya kadar her şey için [kütüphaneler](https://github.com/deimo-s/serenity/blob/master/Userland/Libraries)
- Birçok yaygın ve yaygın olmayan dosya biçimi desteği (PNG, JPEG, GIF, MP3, WAV, FLAC, ZIP, TAR, PDF, QOI, Gemini, ...)
- Birleşik stil ve tasarım felsefesi, esnek tema sistemi, [özel (bit eşlem ve vektör) yazı tipleri](https://fonts.serenityos.net/font-family)
- [Oyunlar](https://github.com/deimo-s/serenity/blob/master/Userland/Games) (Solitaire, Minesweeper, 2048, satranç, Conway'in Hayat Oyunu, ...) ve [demolar](https://github.com/deimo-s/serenity/blob/master/Userland/Demos) (CatDog, Starfield, Eyes, Mandelbrot kümesi, WidgetGallery, ...)
- Günlük GUI programları ve yardımcı programlar (JavaScript destekli Hesap Tablosu, TextEditor, Terminal, PixelPaint, çeşitli multimedya görüntüleyiciler ve oynatıcılar, Mail, Assistant, Hesap Makinesi, ...)

... ve yukarıdakilerin hepsi bu depoda, ekstra bağımlılık yok, tarafımızca sıfırdan geliştirildi :^)

Ek olarak, oyunlar, derleyiciler, Unix araçları, multimedya uygulamaları ve daha fazlasını içeren [üç yüzden fazla popüler açık kaynaklı yazılım portu](https://github.com/deimo-s/serenity/blob/master/Ports/AvailablePorts.md) bulunmaktadır.

## Belgeleri nasıl okurum?

Man sayfaları çevrimiçi olarak [man.serenityos.org](https://man.serenityos.org) adresinde mevcuttur. Bu sayfalar [`Base/usr/share/man`](https://github.com/SerenityOS/serenity/tree/master/Base/usr/shareman) içindeki Markdown kaynak dosyalarından oluşturulur ve otomatik olarak güncellenir.

SerenityOS çalıştırırken terminal arayüzü için `man`, GUI için `help` kullanabilirsiniz.

Kodla ilgili belgeler [belgeler](https://github.com/deimo-s/serenity/blob/master/Documentation) klasöründe bulunabilir.

## Derleyip çalıştırmak

[SerenityOS derleme talimatlarına](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructions.md) veya [Ladybird derleme talimatlarına](https://github.com/deimo-s/serenity/blob/master/Documentation/BuildInstructionsLadybird.md) bakın.

Derleme sistemi, Linux, macOS, Windows (WSL2 ile) ve diğer birçok \*Nix'ten SerenityOS'un çapraz derlemesini destekler. Varsayılan derleme sistemi komutları, desteklendiği şekilde donanım veya yazılım sanallaştırma etkinken OS çalıştıran bir QEMU örneğini başlatır.

Ladybird, SerenityOS'un çapraz derlemesi için ana makine olabilen aynı platformlarda ve SerenityOS'un kendisinde çalışır.

## İletişime geçin ve katılın!

Discord sunucumuza katılın: [SerenityOS Discord](https://serenityos.org/discord)

Bir sorun açmadan önce, lütfen [sorun politikasına](https://github.com/SerenityOS/serenity/blob/master/CONTRIBUTING.md#issue-policy) bakın.

Katkıda bulunmak için genel bir kılavuz [`CONTRIBUTING.md`](https://github.com/deimo-s/serenity/blob/master/CONTRIBUTING.md) dosyasında bulunabilir.

## Yazarlar

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

Ve daha pek çok kişi! Tam katkıda bulunanlar listesi için [buraya bakın](https://github.com/SerenityOS/serenity/graphs/contributors). Yukarıda listelenen kişiler projede 100'den fazla commit gerçekleştirmiştir. :^)

## Lisans

SerenityOS, 2 maddeli BSD lisansı altında lisanslanmıştır.
