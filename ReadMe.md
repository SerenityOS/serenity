# Serenity

x86 Unix-like operating system for IBM PC-compatibles.

## About

I always wondered what it would be like to write my own operating system, but I never took it seriously. Until now.

I've grown tired of cutesy and condescending software that doesn't take itself or the user seriously. This is my effort to bring back the feeling of computing I once knew.

Roughly speaking, the goal here is a marriage between the aesthetic of late-1990s productivity software and the power-user accessibility of late-2000s \*nix. This is a system by me, for me, based on the things I like.

## Screenshot

![Screenshot as of cdb82f6](https://raw.githubusercontent.com/awesomekling/serenity/master/Meta/screenshot-cdb82f6.png)

## Current features

* Pre-emptive multitasking
* Compositing window server
* IPv4 networking with ARP, TCP, UDP and ICMP
* ext2 filesystem
* Unix-like libc and userland
* mmap()
* /proc filesystem
* Local sockets
* Pseudoterminals
* Event-driven GUI library
* Text editor
* IRC client
* DNS lookup
* Other stuff I can't think of right now...

## How do I build and run this?

You need a freestanding cross-compiler for the i686-elf target (for the kernel) and another
cross-compiler for the i686-pc-serenity target (for all the userspace stuff.) It's probably possible to coerce it into building with vanilla gcc/clang if you pass all the right compiler flags, but I haven't been doing that for a while.

There's [a helpful guide on building a GCC cross-compiler](https://wiki.osdev.org/GCC_Cross-Compiler) on the OSDev wiki.

I've only tested this on an Ubuntu 18.10 host with GCC 8.2.0, so I'm not sure it works anywhere else.

If you'd like to run it, here's how you'd get it to boot:

    cd Kernel
    ./makeall.sh
    ./run            # Runs in QEMU
    ./run b          # Runs in bochs (limited networking support)

## Author

* **Andreas Kling** - [awesomekling](https://github.com/awesomekling)

## License

Undecided. Probably something close to 2-clause BSD.
