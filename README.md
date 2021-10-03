# SerenityOS

Graphical Unix-like operating system for x86 computers.

[![GitHub Actions Status](https://github.com/SerenityOS/serenity/workflows/Build,%20lint,%20and%20test/badge.svg)](https://github.com/SerenityOS/serenity/actions?query=workflow%3A"Build%2C%20lint%2C%20and%20test")
[![Azure DevOps Status](https://dev.azure.com/SerenityOS/SerenityOS/_apis/build/status/CI?branchName=master)](https://dev.azure.com/SerenityOS/SerenityOS/_build/latest?definitionId=1&branchName=master)
[![Fuzzing Status](https://oss-fuzz-build-logs.storage.googleapis.com/badges/serenity.svg)](https://bugs.chromium.org/p/oss-fuzz/issues/list?sort=-opened&can=1&q=proj:serenity)
[![Sonar Cube Static Analysis](https://sonarcloud.io/api/project_badges/measure?project=SerenityOS_serenity&metric=ncloc)](https://sonarcloud.io/dashboard?id=SerenityOS_serenity)
[![Discord](https://img.shields.io/discord/830522505605283862.svg?logo=discord&logoColor=white&logoWidth=20&labelColor=7289DA&label=Discord&color=17cf48)](https://discord.gg/serenityos)

## About

SerenityOS is a love letter to '90s user interfaces with a custom Unix-like core. It flatters with sincerity by stealing beautiful ideas from various other systems.

Roughly speaking, the goal is a marriage between the aesthetic of late-1990s productivity software and the power-user accessibility of late-2000s \*nix. This is a system by us, for us, based on the things we like.

I (Andreas) regularly post raw hacking sessions and demos on [my YouTube channel](https://www.youtube.com/c/AndreasKling/).

Sometimes I write about the system on [my github.io blog](https://awesomekling.github.io/).

I'm also on [Patreon](https://www.patreon.com/serenityos) and [GitHub Sponsors](https://github.com/sponsors/awesomekling) if you would like to show some support that way.

## Screenshot

![Screenshot as of b36968c.png](https://raw.githubusercontent.com/SerenityOS/serenity/master/Meta/screenshot-b36968c.png)

## Kernel features

* x86 (32-bit) and x86_64 (64-bit) kernel with pre-emptive multi-threading
* Hardware protections (SMEP, SMAP, UMIP, NX, WP, TSD, ...)
* IPv4 stack with ARP, TCP, UDP and ICMP protocols
* ext2 filesystem
* POSIX signals
* Purgeable memory
* /proc filesystem
* Pseudoterminals (with /dev/pts filesystem)
* Filesystem notifications
* CPU and memory profiling
* SoundBlaster 16 driver
* VMWare/QEMU mouse integration

## System services

* Launch/session daemon (SystemServer)
* Compositing window server (WindowServer)
* Text console manager (TTYServer)
* DNS client (LookupServer)
* Network protocols server (RequestServer and WebSocket)
* Software-mixing sound daemon (AudioServer)
* Desktop notifications (NotificationServer)
* HTTP server (WebServer)
* Telnet server (TelnetServer)
* DHCP client (DHCPClient)

## Libraries

* C++ templates and containers (AK)
* Event loop and utilities (LibCore)
* 2D graphics library (LibGfx)
* OpenGL 1.x compatible library (LibGL)
* GUI toolkit (LibGUI)
* Cross-process communication library (LibIPC)
* HTML/CSS engine (LibWeb)
* JavaScript engine (LibJS)
* Markdown (LibMarkdown)
* Audio (LibAudio)
* Digital Signal Processing/Synthesizer Chains (LibDSP)
* PCI database (LibPCIDB)
* Terminal emulation (LibVT)
* Out-of-process network protocol I/O (LibProtocol)
* Mathematical functions (LibM)
* ELF file handling (LibELF)
* POSIX threading (LibPthread)
* Higher-level threading (LibThreading)
* Transport Layer Security (LibTLS)
* HTTP and HTTPS (LibHTTP)
* IMAP (LibIMAP)

## Userland features

* Unix-like libc and userland
* Shell with pipes and I/O redirection
* On-line help system (both terminal and GUI variants)
* Web browser (Browser)
* C++ IDE (HackStudio)
* Desktop synthesizer (Piano)
* E-mail client (Mail)
* Various desktop apps & games
* Color themes

## How do I read the documentation?

Man pages are available online at [man.serenityos.org](https://man.serenityos.org). These pages are generated from the Markdown source files in [`Base/usr/share/man`](https://github.com/SerenityOS/serenity/tree/master/Base/usr/share/man) and updated automatically.

When running SerenityOS you can use `man` for the terminal interface, or `help` for the GUI.

## How do I build and run this?

See the [SerenityOS build instructions](https://github.com/SerenityOS/serenity/blob/master/Documentation/BuildInstructions.md)

## Before opening an issue

Please see the [issue policy](https://github.com/SerenityOS/serenity/blob/master/CONTRIBUTING.md#issue-policy).

FAQ: [Frequently Asked Questions](https://github.com/SerenityOS/serenity/blob/master/Documentation/FAQ.md)

## Get in touch

Join our Discord server: [SerenityOS Discord](https://discord.gg/serenityos)

## Author

* **Andreas Kling** - [awesomekling](https://twitter.com/awesomekling)

## Contributors

* **Robin Burchell** - [rburchell](https://github.com/rburchell)
* **Conrad Pankoff** - [deoxxa](https://github.com/deoxxa)
* **Sergey Bugaev** - [bugaevc](https://github.com/bugaevc)
* **Liav A** - [supercomputer7](https://github.com/supercomputer7)
* **Linus Groh** - [linusg](https://github.com/linusg)
* **Ali Mohammad Pur** - [alimpfard](https://github.com/alimpfard)
* **Shannon Booth** - [shannonbooth](https://github.com/shannonbooth)
* **Hüseyin ASLITÜRK** - [asliturk](https://github.com/asliturk)
* **Matthew Olsson** - [mattco98](https://github.com/mattco98)
* **Nico Weber** - [nico](https://github.com/nico)
* **Brian Gianforcaro** - [bgianfo](https://github.com/bgianfo)
* **Ben Wiederhake** - [BenWiederhake](https://github.com/BenWiederhake)
* **Tom** - [tomuta](https://github.com/tomuta)
* **Paul Scharnofske** - [asynts](https://github.com/asynts)
* **Itamar Shenhar** - [itamar8910](https://github.com/itamar8910)
* **Luke Wilde** - [Lubrsi](https://github.com/Lubrsi)
* **Brendan Coles** - [bcoles](https://github.com/bcoles)
* **Andrew Kaster** - [ADKaster](https://github.com/ADKaster)
* **thankyouverycool** - [thankyouverycool](https://github.com/thankyouverycool)
* **Idan Horowitz** - [IdanHo](https://github.com/IdanHo)
* **Gunnar Beutner** - [gunnarbeutner](https://github.com/gunnarbeutner)
* **Tim Flynn** - [trflynn89](https://github.com/trflynn89)
* **Jean-Baptiste Boric** - [boricj](https://github.com/boricj)
* **Stephan Unverwerth** - [sunverwerth](https://github.com/sunverwerth)
* **Max Wipfli** - [MaxWipfli](https://github.com/MaxWipfli)
* **Daniel Bertalan** - [BertalanD](https://github.com/BertalanD)
* **Jelle Raaijmakers** - [GMTA](https://github.com/GMTA)
* **Sam Atkins** - [AtkinsSJ](https://github.com/AtkinsSJ)
* **Tobias Christiansen** - [TobyAsE](https://github.com/TobyAsE)
* **Lenny Maiorani** - [ldm5180](https://github.com/ldm5180)
* **sin-ack** - [sin-ack](https://github.com/sin-ack)
* **Jesse Buhagiar** - [Quaker762](https://github.com/Quaker762)
* **Peter Elliott** - [Petelliott](https://github.com/Petelliott)
* **Karol Kosek** - [krkk](https://github.com/krkk)
* **Mustafa Quraish** - [mustafaquraish](https://github.com/mustafaquraish)
* **David Tuin** - [davidot](https://github.com/davidot)

(And many more!) The people listed above have landed more than 100 commits in the project. :^)

## License

SerenityOS is licensed under a 2-clause BSD license.
