## SerenityOS build instructions

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
brew tap discoteq/discoteq
brew install flock
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
- `flock` command can also be installed with `brew install util-linux` but in that case you will need to add it to `$PATH`
- qemu is needed to run the compiled OS image. You can also build it using the `BuildQemu.sh` script
- osxfuse, e2fsprogs, m4, autoconf, automake, libtool and `BuildFuseExt2.sh` are needed if you want to build the root filesystem disk image natively on macOS. This allows mounting an EXT2 fs and also installs commands like `mke2fs` that are not available on stock macOS. 
- If you install some commercial EXT2 macOS fs handler instead of osxfuse and fuse-ext2, you will need to `brew install e2fsprogs` to obtain `mke2fs` anyway.

### OpenBSD prerequisites
```
pkg_add bash gmp gcc git flock gmake sudo
```

When building with `make`, `gmake` must be used.  The `makeall.sh` script will do this automatically when building on OpenBSD.

### Build
Go into the `Toolchain/` directory and run the **BuildIt.sh** script.

Once you've built the toolchain, go into the `Kernel/` directory, then run
**./makeall.sh**, and if nothing breaks too much, take it for a spin by using
**./run**.

You can vastly reduce the build time of successive rebuilds of Serenity by installing `ccache` and `export`ing ```PRE_CXX=ccache```

Bare curious users may even consider sourcing suitable hardware to [install Serenity on a physical PC.](https://github.com/SerenityOS/serenity/blob/master/INSTALL.md)

Later on, when you `git pull` to get the latest changes, there's no need to rebuild the toolchain. You can simply rerun **./makeall.sh** in the `Kernel/` directory and you'll be good to **./run** again.
