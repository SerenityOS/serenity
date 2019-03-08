# Serenity

x86 Unix-like operating system for IBM PC-compatibles.

## About

I always wondered what it would be like to write my own operating system, but I never took it seriously. Until now.

I've grown tired of cutesy and condescending software that doesn't take itself or the user seriously. This is my effort to bring back the feeling of computing we once knew.

## Screenshot

![Screenshot as of b5521e1](https://raw.githubusercontent.com/awesomekling/serenity/master/Meta/screenshot-b5521e1.png)

## Current features

* Pre-emptive multitasking
* Compositing window server
* ext2 filesystem
* Unix-like libc and userland
* mmap()
* /proc filesystem
* Local sockets
* Pseudoterminals
* Event-driven GUI library
* Graphical text editor
* Other stuff I can't think of right now...

## How do I get it to run?

You need a freestanding cross-compiler for the i686-elf target (for the kernel) and another
cross-compiler for the i686-pc-serenity target (for all the userspace stuff.) It's probably possible to coerce it into building with vanilla gcc/clang if you pass all the right compiler flags, but I haven't been doing that for a while.

I've only tested this on an Ubuntu 18.10 host with GCC 8.2.0, so I'm not sure it works anywhere else.

If you'd like to run it, here's how you'd get it to boot:

    cd Kernel
    ./makeall.sh
    ./run q          # Runs in QEMU
    ./run            # Runs in bochs

## Author

* **Andreas Kling** - [awesomekling](https://github.com/awesomekling)

## License

Undecided. Probably something close to 2-clause BSD.
