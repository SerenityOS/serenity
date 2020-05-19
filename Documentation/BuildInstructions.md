## SerenityOS build instructions

### Prerequisites

#### Linux prerequisites
Make sure you have all the dependencies installed:

**Debian / Ubuntu**
```bash
sudo apt install build-essential cmake curl libmpfr-dev libmpc-dev libgmp-dev e2fsprogs qemu-system-i386 qemu-utils
```

On Docker, install these as well:
```bash
sudo apt install wget genext2fs
```

**Fedora**
```bash
sudo dnf install curl cmake mpfr-devel libmpc-devel gmp-devel e2fsprogs @"C Development Tools and Libraries" @Virtualization
```

**Arch Linux / Manjaro**
```bash
sudo pacman -S base-devel cmake curl mpfr libmpc gmp e2fsprogs qemu qemu-arch-extra
```

Ensure your gcc version is >= 8 with `gcc --version`. Otherwise, install it (on Ubuntu) with:
```bash
sudo add-apt-repository ppa:ubuntu-toolchain-r/test
sudo apt-get install gcc-9 g++-9
sudo update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-9 900 --slave /usr/bin/g++ g++ /usr/bin/g++-9
```

#### macOS prerequisites
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
brew install bash
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
- bash is needed because the default version installed on macOS doesn't support globstar
- If you install some commercial EXT2 macOS fs handler instead of osxfuse and fuse-ext2, you will need to `brew install e2fsprogs` to obtain `mke2fs` anyway.

#### OpenBSD prerequisites
```
pkg_add bash gmp gcc git flock gmake sudo
```

#### FreeBSD prerequisites
```
$ pkg add coreutils gmake bash sudo git
$ ln -s /usr/local/bin/ginstall /usr/local/bin/install
```

### Build
> Before starting, make sure that you have configured your global identity for git, or the first script will fail after running for a bit.

Go into the `Toolchain/` directory and run the **BuildIt.sh** script:
```bash
$ cd Toolchain
$ ./BuildIt.sh
```

Once you've built the toolchain, create a directory for the build to live in (for example, `Build/`), and run the CMake build:
```bash
cd ..
mkdir Build && cd Build
cmake ..
```

Once the toolchain and cmake have been built, run the `make` and `make install` commands:
```bash
$ make
$ make install
```

This will compile all of SerenityOS and install the built files into `Root/` inside the build tree. `make install` actually pulls in the regular `make` (`make all`) automatically, so there isn't really a need to run it explicitly. You may also want ask `make` to build things in parallel by using `-j`, optionally specifying the maximum number of jobs to run.

Now to build a disk image, run `make image`, and if nothing breaks too much, take it for a spin by using `make run`.
```bash
$ make image
$ make run
```

Note that the `anon` user is able to become `root` without password by default, as a development convenience.
To prevent this, remove `anon` from the `wheel` group and he will no longer be able to run `/bin/su`.

On Linux, QEMU is significantly faster if it's able to use KVM. The run script will automatically enable KVM if `/dev/kvm` exists and is readable+writable by the current user.

Bare curious users may even consider sourcing suitable hardware to [install Serenity on a physical PC.](https://github.com/SerenityOS/serenity/blob/master/INSTALL.md)

Later on, when you `git pull` to get the latest changes, there's no need to rebuild the toolchain. You can simply run `make install`, `make image`, `make run` again. CMake will only rebuild those parts that have been updated.

You may also want to replace `make` with `ninja` in the above commands for some additional build speed benefits. To do this, go to an empty directory at the root and call `cmake .. -G Ninja` inside that directory. You might either create a new directory or reuse the existing `Build` directory after cleaning it.

#### Ports
To add a package from the ports collection to Serenity, for example curl, go into `Ports/curl/` and run **./package.sh**. The sourcecode for the package will be downloaded and the package will be built. After that, run **make image** from the `Build/` directory to update the disk image. The next time you start Serenity with **make run**, `curl` will be available.
