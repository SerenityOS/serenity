# Serenity

x86 Unix-like operating system for IBM PC-compatibles.

## About

I always wanted to write my own operating system, but I never took it seriously. Until now.

## Screenshot

![Screenshot as of 23263aa](https://raw.githubusercontent.com/awesomekling/serenity/master/Meta/screenshot-23263aa.png)

## Current features

* Pre-emptive multitasking
* Compositing window server (in userspace)
* ext2 filesystem
* Unix-like libc and userland
* mmap()
* /proc filesystem
* Local sockets
* Pseudoterminals
* Event-driven GUI library
* Other stuff I can't think of right now...

## How do I get it to run?

You need a freestanding cross-compiler for the i686-elf target (for the kernel) and another
cross-compiler for the i686-pc-serenity target (for all the userspace stuff.)

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
