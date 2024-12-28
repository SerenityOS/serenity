# SerenityOS Build Instructions

## Prerequisites

Make sure you have all the dependencies installed:

### Debian / Ubuntu

```console
sudo apt install build-essential cmake curl libmpfr-dev libmpc-dev libgmp-dev e2fsprogs ninja-build qemu-system-gui qemu-system-x86 qemu-utils ccache rsync unzip texinfo libssl-dev
```

Optional: `fuse2fs` for [building images without root](https://github.com/SerenityOS/serenity/pull/11224).

#### GCC 13 or Clang 17+

A host compiler that supports C++23 features is required for building host tools, the newer the better. Tested versions include gcc-13 and Clang 17 through 19.

On Ubuntu gcc-13 is available in the repositories of 24.04 (Noble) and later.
If you are running an older version, you will either need to upgrade, or find an alternative installation source
(i.e. from the [ubuntu-toolchain-r/test PPA](https://launchpad.net/~ubuntu-toolchain-r/+archive/ubuntu/test)).

On both Ubuntu and Debian, recent versions of Clang are available in the [LLVM apt repositories](https://apt.llvm.org/).

Next, update your local package information from the new repositories:

```console
sudo apt update
```

Now on Ubuntu or Debian you can install gcc-13 with apt like this:

```console
sudo apt install gcc-13 g++-13
```

For Clang, the following packages are required (for example Clang 19). Note that the `-dev` packages are only necessary when jakt is enabled.

```
sudo apt install libclang-19-dev clang-19 llvm-19 llvm-19-dev
```

#### QEMU 6.2 or later

Version 6.2 of QEMU is available in Ubuntu 22.04. On earlier versions of Ubuntu,
you can build the recommended version of QEMU as provided by the toolchain by running
`Toolchain/BuildQemu.sh`.
Note that you might need additional dev packages in order to build QEMU on your machine:

```console
sudo apt install libgtk-3-dev libpixman-1-dev libsdl2-dev libslirp-dev libspice-server-dev
```

#### CMake version 3.25.0 or later

Serenity-specific patches were upstreamed to CMake in major version 3.25. To avoid carrying
patches to CMake, the minimum required CMake to build Serenity is set to that version.
If more patches are upstreamed to CMake, the minimum will be bumped again once that version releases.

To accommodate distributions that do not ship bleeding-edge CMake versions, the build scripts will
attempt to build CMake from source if the version on your path is older than 3.25.x.

If you have previously compiled SerenityOS with an older or distribution-provided version of CMake,
you will need to manually remove the CMakeCache.txt files, as these files reference the older CMake version and path.

```console
rm Build/*/CMakeCache.txt
```

### Windows

If you're on Windows you can use WSL2 to build SerenityOS. Please have a look at the [Windows guide](BuildInstructionsWindows.md)
for details.

### Arch Linux / Manjaro

```console
sudo pacman -S --needed base-devel cmake curl mpfr libmpc gmp e2fsprogs ninja qemu-desktop qemu-system-aarch64 ccache rsync unzip
```

Optional: `fuse2fs` for [building images without root](https://github.com/SerenityOS/serenity/pull/11224), and `clang llvm llvm-libs` for building with Clang.

### SerenityOS

The following ports need to be installed:

```console
bash cmake curl e2fsprogs gawk genext2fs git ninja patch python3 qemu rsync
```

Additionally, for building using LLVM, install the `llvm` port.
For building using GCC, install the `gcc`, `gmp` and `mpc` ports.

Due to not-yet-finished POSIX shell support in `Shell`, a symlink from `/bin/sh` to `/usr/local/bin/bash` is required.
This is best achieved by adding `ln -sf /usr/local/bin/bash mnt/bin/sh` to your [customization script](AdvancedBuildInstructions.md#customizing-the-disk-image).

### Other systems

There is also documentation for installing the build prerequisites for some less commonly used systems:

-   [Other Linux distributions and \*NIX systems](BuildInstructionsOther.md)
-   [macOS](BuildInstructionsMacOS.md)

## Build

Run the following command to build and run SerenityOS:

```console
Meta/serenity.sh run
```

This will compile all of SerenityOS and install the built files into the `Build/<architecture>/Root` directory inside your Git
repository. It will also build a disk image and start SerenityOS using QEMU. The chosen architecture defaults to
your host architecture. Supported architectures are x86_64, aarch64 and riscv64.

The first time this command is executed, it will also download some required database files from the internet and build
the SerenityOS cross-compiler toolchain. These steps only have to be done once, so the next build will go much faster.
When we update to a newer compiler, you might be prompted to re-build the toolchain; see the [troubleshooting guide](Troubleshooting.md#the-toolchain-is-outdated)
for what to do when this happens.

If, during build, an error like `fusermount: failed to open /etc/mtab: No such file or directory` appears, you have installed `fuse2fs` but your system does not provide the mtab symlink for various reasons. Simply create this symlink with `ln -sv /proc/self/mounts /etc/mtab`.

Note that the `anon` user is able to become `root` without a password by default, as a development convenience.
To prevent this, remove `anon` from the `wheel` group and it will no longer be able to run `/bin/su`.

By default the `anon` user account's password is: `foo`

If you want to test whether your code changes compile without running the VM you can use
`Meta/serenity.sh build`. The `serenity.sh` script also provides a number of other commands. Run the script without
arguments for a list.

## Ports

To add a package from the ports collection to Serenity, for example curl, change into the `Ports/curl` directory and
run `./package.sh`. The source code for the package will be downloaded and the package will be built. The next time you
start Serenity, `curl` will be available.

Ports might also have additional dependencies. Most prominently, you may need:
`autoconf`, `automake`, `bison`, `flex`, `gettext`, `gperf`, `help2man`, `imagemagick` (specifically "convert"),
`libgpg-error-dev`, `libtool`, `lzip`, `meson`, `nasm` (or another assembler), `python3-packaging`, `qt6-base-dev`,
`rename`, `zip`.

For select ports you might need slightly more exotic dependencies such as:

-   `file` (version 5.44 exactly, for file)
-   `libpython3-dev` (most prominently for boost)
-   `lua` (for luarocks)
-   `openjdk-17-jdk` (to compile OpenJDK)
-   `rake` (to build mruby).

You may also need a symlink from "/usr/bin/python" to "/usr/bin/python3"; some ports depend on "python" existing, most notably ninja.

## More information

At this point you should have a fully functioning VM for SerenityOS. The [advanced build instructions guide](AdvancedBuildInstructions.md)
has more information for some less commonly used features of the build system.
