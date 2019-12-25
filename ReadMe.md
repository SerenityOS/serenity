# Serenity

Graphical Unix-like operating system for x86 computers.

![Travis CI status](https://api.travis-ci.com/SerenityOS/serenity.svg?branch=master)

## About

*I always wondered what it would be like to write my own operating system, but I never took it seriously. Until now.*

Serenity is a love letter to '90s user interfaces with a custom Unix-like core. It flatters with sincerity by stealing beautiful ideas from various other systems.

Roughly speaking, the goal is a marriage between the aesthetic of late-1990s productivity software and the power-user accessibility of late-2000s \*nix. This is a system by me, for me, based on the things I like.

If you like some of the same things, you are welcome to join the project. It would be great to one day change the above to say "this is a system by us, for us, based on the things we like." :^)

I regularly post raw hacking sessions and demos on [my YouTube channel](https://www.youtube.com/c/AndreasKling/).

Sometimes I write about the system on [my github.io blog](https://awesomekling.github.io/).

There's also a [Patreon](https://www.patreon.com/serenityos) if you would like to show some support that way.

## Screenshot

![Screenshot as of 1133aca](https://raw.githubusercontent.com/SerenityOS/serenity/master/Meta/screenshot-1133aca.png)

## Current features (all under development)

* Pre-emptive multitasking
* Multithreading
* Compositing window server
* IPv4 networking with ARP, TCP, UDP and ICMP
* ext2 filesystem
* Unix-like libc and userland
* POSIX signals
* Shell with pipes and I/O redirection
* mmap()
* /proc filesystem
* Local sockets
* Pseudoterminals (with /dev/pts filesystem)
* Filesystem notifications
* JSON framework
* Low-level utility library (LibCore)
* Mid-level 2D graphics library (LibDraw)
* High-level GUI library (LibGUI)
* HTML/CSS engine
* Web browser
* C++ IDE
* Emojis (UTF-8)
* HTTP downloads
* SoundBlaster 16 driver
* Software-mixing sound daemon
* WAV playback
* Simple desktop piano/synthesizer
* Visual GUI design tool
* PNG format support
* Text editor
* IRC client
* Simple painting application
* DNS lookup
* Desktop games: Minesweeper and Snake
* Ports system (needs more packages!)
* Other stuff I can't think of right now...

## How do I build and run this?

### Linux prerequisites
Make sure you have all the dependencies installed:

```bash
sudo apt install build-essential curl libmpfr-dev libmpc-dev libgmp-dev e2fsprogs qemu-system-i386 qemu-utils
```

Ensure your gcc version is >= 8 with `gcc --version`. Otherwise, install it (on Ubuntu) with:
```bash
sudo add-apt-repository ppa:ubuntu-toolchain-r/test
sudo apt-get install gcc-8 g++-8
sudo update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-8 800 --slave /usr/bin/g++ g++ /usr/bin/g++-8
```

### macOS prerequisites
Make sure you have all the dependencies installed:
```bash
brew install coreutils
brew install qemu
brew install wget
brew install e2fsprogs
brew install m4
brew install autoconf
brew install libtool
brew install automake
brew cask install osxfuse
Toolchain/BuildFuseExt2.sh
```

Notes: 
- fuse-ext2 is not available as brew formula so it must be installed using `BuildFuseExt2.sh`
- Xcode and `xcode-tools` must be installed (`git` is required by some scripts)
- coreutils is needed to build gcc cross compiler
- qemu is needed to run the compiled OS image. You can also build it using the `BuildQemu.sh` script
- osxfuse, e2fsprogs, m4, autoconf, automake, libtool and `BuildFuseExt2.sh` are needed if you want to build the root filesystem disk image natively on macOS. This allows mounting an EXT2 fs and also installs commands like `mke2fs` that are not available on stock macOS. 
- If you install some commercial EXT2 macOS fs handler instead of osxfuse and fuse-ext2, you will need to `brew install e2fsprogs` to obtain `mke2fs` anyway.

### Build
Go into the `Toolchain/` directory and run the **BuildIt.sh** script.

Once you've built the toolchain, go into the `Kernel/` directory, then run
**./makeall.sh**, and if nothing breaks too much, take it for a spin by using
**./run**.

You can vastly reduce the build time of successive rebuilds of Serenity by installing `ccache` and `export`ing ```PRE_CXX=ccache```

Bare curious users may even consider sourcing suitable hardware to [install Serenity on a physical PC.](https://github.com/SerenityOS/serenity/blob/master/INSTALL.md)

Later on, when you `git pull` to get the latest changes, there's no need to rebuild the toolchain. You can simply rerun **./makeall.sh** in the `Kernel/` directory and you'll be good to **./run** again.

## IRC

Come chat in `#serenityos` on the Freenode IRC network.

## Author

* **Andreas Kling** - [awesomekling](https://twitter.com/awesomekling)

## Contributors

* **Robin Burchell** - [rburchell](https://github.com/rburchell)
* **Conrad Pankoff** - [deoxxa](https://github.com/deoxxa)
* **Sergey Bugaev** - [bugaevc](https://github.com/bugaevc)

(And many more!) Feel free to append yourself here if you've made some sweet contributions. :)

## License

Serenity is licensed under a 2-clause BSD license.
