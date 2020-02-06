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

![Screenshot as of 1133aca](https://raw.githubusercontent.com/SerenityOS/serenity/master/Meta/screenshot-1133aca.png)

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
* DNS client (LookupServer)
* Software-mixing sound daemon (AudioServer)

## Libraries

* C++ templates and containers (AK)
* Event loop and utilities (LibCore)
* 2D graphics library (LibGfx)
* GUI toolkit (LibGUI)
* Cross-process communication library (LibIPC)
* HTML/CSS engine (LibHTML)
* Markdown (LibMarkdown)
* Audio (LibAudio)
* PCI database (LibPCIDB)
* Terminal emulation (LibVT)
* Network protocols (HTTP) (LibProtocol)

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

## How do I build and run this?

See the [SerenityOS build instructions](https://github.com/SerenityOS/serenity/blob/master/Documentation/BuildInstructions.md)

## Wanna chat?

Come hang out with us in `#serenityos` on the Freenode IRC network.

## Author

* **Andreas Kling** - [awesomekling](https://twitter.com/awesomekling)

## Contributors

* **Robin Burchell** - [rburchell](https://github.com/rburchell)
* **Conrad Pankoff** - [deoxxa](https://github.com/deoxxa)
* **Sergey Bugaev** - [bugaevc](https://github.com/bugaevc)

(And many more!) Feel free to append yourself here if you've made some sweet contributions. :)

## License

SerenityOS is licensed under a 2-clause BSD license.
