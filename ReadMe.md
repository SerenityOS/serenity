# SerenityOS

Graphical Unix-like operating system for x86 computers.

![Travis CI status](https://api.travis-ci.com/SerenityOS/serenity.svg?branch=master)

## About

SerenityOS is a love letter to '90s user interfaces with a custom Unix-like core. It flatters with sincerity by stealing beautiful ideas from various other systems.

Roughly speaking, the goal is a marriage between the aesthetic of late-1990s productivity software and the power-user accessibility of late-2000s \*nix. This is a system by us, for us, based on the things we like.

I (Andreas) regularly post raw hacking sessions and demos on [my YouTube channel](https://www.youtube.com/c/AndreasKling/).

Sometimes I write about the system on [my github.io blog](https://awesomekling.github.io/).

I'm also on [Patreon](https://www.patreon.com/serenityos) and [GitHub Sponsors](https://github.com/sponsors/awesomekling) if you would like to show some support that way.

## Screenshot

![Screenshot as of 8ea4375](https://raw.githubusercontent.com/SerenityOS/serenity/master/Meta/screenshot-8ea4375.png)

## Kernel features

* x86 (32-bit) kernel with pre-emptive multi-threading
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
* Network protocols server (ProtocolServer)
* Software-mixing sound daemon (AudioServer)
* Desktop notifications (NotificationServer)
* HTTP server (WebServer)
* Telnet server (TelnetServer)
* DHCP client (DHCPClient)

## Libraries

* C++ templates and containers (AK)
* Event loop and utilities (LibCore)
* 2D graphics library (LibGfx)
* GUI toolkit (LibGUI)
* Cross-process communication library (LibIPC)
* HTML/CSS engine (LibWeb)
* JavaScript engine (LibJS)
* Markdown (LibMarkdown)
* Audio (LibAudio)
* PCI database (LibPCIDB)
* Terminal emulation (LibVT)
* Out-of-process network protocol I/O (LibProtocol)
* Mathematical functions (LibM)
* ELF file handling (LibELF)
* POSIX threading (LibPthread)
* Higher-level threading (LibThread)
* Transport Layer Security (LibTLS)
* HTTP and HTTPS (LibHTTP)

## Userland features

* Unix-like libc and userland
* Shell with pipes and I/O redirection
* On-line help system (both terminal and GUI variants)
* Web browser (Browser)
* C++ IDE (HackStudio)
* IRC client
* Desktop synthesizer (Piano)
* Various desktop apps & games
* Color themes

## How do I read the documentation?

Man pages are browsable outside of SerenityOS under [Base/usr/share/man](https://github.com/SerenityOS/serenity/tree/master/Base/usr/share/man).

When running SerenityOS you can use `man` for the terminal interface, or `help` for the GUI interface.

## How do I build and run this?

See the [SerenityOS build instructions](https://github.com/SerenityOS/serenity/blob/master/Documentation/BuildInstructions.md)

## Before opening an issue

Please see the [issue policy](https://github.com/SerenityOS/serenity/blob/master/CONTRIBUTING.md#issue-policy).

## Communication hubs

The main hub is `#serenityos` on the Freenode IRC network.

We also have a project mailing list: [serenityos-dev](https://lists.sr.ht/~awesomekling/serenityos-dev).

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

(And many more!) The people listed above have landed more than 100 commits in the project. :^)

## License

SerenityOS is licensed under a 2-clause BSD license.
