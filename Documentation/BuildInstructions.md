# SerenityOS build instructions

## Prerequisites

### Linux prerequisites

Ensure your CMake version is >= 3.16 with `cmake --version`. If your system doesn't provide a suitable version of CMake, you can download a binary release from the [CMake website](https://cmake.org/download).

Ensure your gcc version is >= 10 with `gcc --version`. Otherwise, install it.

On Ubuntu it's in the repositories of 20.04 (Focal) and later - add the `ubuntu-toolchain-r/test` PPA if you're running an older version:

```console
sudo add-apt-repository ppa:ubuntu-toolchain-r/test
```

On Debian you can use the Debian testing branch:

```console
sudo echo "deb http://http.us.debian.org/debian/ testing non-free contrib main" >> /etc/apt/sources.list
sudo apt update
```

Now on Ubuntu or Debian you can install gcc-10 with apt like this:

```console
sudo apt install gcc-10 g++-10
sudo update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-10 900 --slave /usr/bin/g++ g++ /usr/bin/g++-10
```

If you don't want to stay on the Debian testing branch you can switch back by running:

```console
sudo sed -i '$d' /etc/apt/sources.list
sudo apt update
```

Make sure you have all the dependencies installed (`ninja` is optional, but is faster in practice):

#### Debian / Ubuntu

```console
sudo apt install build-essential cmake curl libmpfr-dev libmpc-dev libgmp-dev e2fsprogs ninja-build qemu-system-i386 qemu-utils
```

#### Fedora

```console
sudo dnf install binutils-devel curl cmake mpfr-devel libmpc-devel gmp-devel e2fsprogs ninja-build patch @"C Development Tools and Libraries" @Virtualization
```

#### openSUSE

```console
sudo zypper install curl cmake mpfr-devel mpc-devel ninja gmp-devel e2fsprogs patch qemu-x86 qemu-audio-pa gcc gcc-c++ patterns-devel-C-C++-devel_C_C++
```

#### Arch Linux / Manjaro

```console
sudo pacman -S --needed base-devel cmake curl mpfr libmpc gmp e2fsprogs ninja qemu qemu-arch-extra
```

#### ALT Linux

```console
apt-get install curl cmake libmpc-devel gmp-devel e2fsprogs libmpfr-devel ninja-build patch gcc
```

#### NixOS

You can use a `nix-shell` script like the following to set up the correct environment:

myshell.nix:

```
with import <nixpkgs> {};

stdenv.mkDerivation {
  name = "cpp-env";
  nativeBuildInputs = [
    gcc10
    curl
    cmake
    mpfr
    ninja
    gmp
    libmpc
    e2fsprogs
    patch

    # Example Build-time Additional Dependencies
    pkgconfig
  ];
  buildInputs = [
    # Example Run-time Additional Dependencies
    openssl
    x11
    # glibc
  ];
  hardeningDisable = [ "format" "fortify" ];
}
```

Then use this script: `nix-shell myshell.nix`.

Once you're in nix-shell, you should be able to follow the build directions.

#### Alpine Linux

First, make sure you have enabled the `community` repository in `/etc/apk/repositories` and run `apk update`. It has been tested on `edge`, YMMV on `stable`.

```console
# the basics, if you have not already done so
apk add bash curl git util-linux sudo

# rough equivalent of build-essential
apk add build-base

# qemu
apk add qemu qemu-system-i386 qemu-img qemu-ui-gtk

# build tools (samurai is a drop-in replacement for ninja)
apk add cmake e2fsprogs grub-bios samurai mpc1-dev mpfr-dev gmp-dev
```

### macOS prerequisites

Make sure you have all the dependencies installed:

```console
# core
brew install coreutils qemu bash gcc@10 ninja cmake

# (option 1) fuse + ext2
brew install e2fsprogs m4 autoconf automake libtool
brew install --cask osxfuse
Toolchain/BuildFuseExt2.sh

# (option 2) genext2fs
brew install genext2fs
```

Notes:

- fuse-ext2 is not available as brew formula so it must be installed using `BuildFuseExt2.sh`
- Xcode and `xcode-tools` must be installed (`git` is required by some scripts)
- coreutils is needed to build gcc cross compiler
- qemu is needed to run the compiled OS image. You can also build it using the `BuildQemu.sh` script
- osxfuse, e2fsprogs, m4, autoconf, automake, libtool and `BuildFuseExt2.sh` are needed if you want to build the root filesystem disk image natively on macOS. This allows mounting an EXT2 fs and also installs commands like `mke2fs` that are not available on stock macOS.
- Installing osxfuse for the first time requires enabling its system extension in System Preferences and then restarting your machine. The output from installing osxfuse with brew says this, but it's easy to miss.
- bash is needed because the default version installed on macOS doesn't support globstar
- If you install some commercial EXT2 macOS fs handler instead of osxfuse and fuse-ext2, you will need to `brew install e2fsprogs` to obtain `mke2fs` anyway.
- As of 2020-08-06, you might need to tell the build system about your newer host compiler. Once you've built the toolchain, navigate to `Build/i686/`, `rm -rf *`, then run `cmake ../.. -G Ninja -DCMAKE_C_COMPILER=gcc-10 -DCMAKE_CXX_COMPILER=g++-10`, then continue with `ninja install` as usual.
- If you are on macOS Big Sur, you will need to manually enable QEMU's acceleration before running Serenity, by creating a new file called `entitlements.xml` in the `Build/` folder, with the content below, and then run the command: `codesign -s - --entitlements entitlements.xml --force /usr/local/bin/qemu-system-x86_64`; otherwise the run command will fail.

<details>
<summary>Content for 'entitlements.xml'.</summary>

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
	"http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>com.apple.security.hypervisor</key>
    <true/>
</dict>
</plist>
```

</details>

### OpenBSD prerequisites

```console
$ doas pkg_add bash cmake g++ gcc git gmake gmp ninja
```

To use `ninja image` and `ninja run`, you'll need Qemu and other utilities:

```console
$ doas pkg_add coreutils qemu sudo
```

### FreeBSD prerequisites

```console
$ pkg install bash coreutils git gmake ninja sudo
```

### Windows prerequisites

For Windows, you will require Windows Subsystem for Linux 2 (WSL2). [Follow the WSL2 instructions here.](https://github.com/SerenityOS/serenity/blob/master/Documentation/NotesOnWSL.md)
Do note the ```Hardware acceleration``` and ```Note on filesystems``` sections, otherwise performance will be terrible.
Once you have installed a distro for WSL2, follow the Linux prerequisites above for the distro you installed, then continue as normal.

You may also want to install [ninja](https://github.com/ninja-build/ninja/releases)

## Build

Go into the `Toolchain/` directory and run the **BuildIt.sh** script:

```console
$ cd Toolchain
$ ./BuildIt.sh
```

Building the toolchain will also automatically create a `Build/i686/` directory for the build to live in. Once the toolchain has been built, go into the `Build/i686/` directory. Specifically, change the current directory to the `Build/` parent directory. To go there from `Toolchain/`, use this command:

```console
$ cd ../Build/i686
```

Run the following commands from within the `Build/i686/` directory. Note that while `ninja` seems to be faster, you can also just use GNU make, by omitting `-G Ninja` and calling `make` instead of `ninja`:

```console
$ cmake ../.. -G Ninja
$ ninja install
```

This will compile all of SerenityOS and install the built files into `Root/` inside the build tree. `ninja` will automatically build as many jobs in parallel as it detects processors; `make` builds only one job in parallel. (Use the `-j` option with an argument if you want to change this.)

Now to build a disk image, run `ninja image`, and take it for a spin by using `ninja run`.

```console
$ ninja image
$ ninja run
```

Note that the `anon` user is able to become `root` without password by default, as a development convenience.
To prevent this, remove `anon` from the `wheel` group and he will no longer be able to run `/bin/su`.

On Linux, QEMU is significantly faster if it's able to use KVM. The run script will automatically enable KVM if `/dev/kvm` exists and is readable+writable by the current user.

Bare curious users may even consider sourcing suitable hardware to [install Serenity on a physical PC.](https://github.com/SerenityOS/serenity/blob/master/Documentation/INSTALL.md)

Outside of QEMU, Serenity will run on VirtualBox and VMware. If you're curious, see how to [install Serenity on VirtualBox](https://github.com/SerenityOS/serenity/blob/master/Documentation/VirtualBox.md) or [install Serenity on VMware](https://github.com/SerenityOS/serenity/blob/master/Documentation/VMware.md).

Later on, when you `git pull` to get the latest changes, there's (usually) no need to rebuild the toolchain. You can simply run `ninja install`, `ninja image`, and `ninja run` again. CMake will only rebuild those parts that have been updated.

### CMake build options

There are some optional features that can be enabled during compilation that are intended to help with specific types of development work or introduce experimental features. Currently, the following build options are available:
- `ENABLE_ADDRESS_SANITIZER` and `ENABLE_KERNEL_ADDRESS_SANITIZER`: builds in runtime checks for memory corruption bugs (like buffer overflows and memory leaks) in Lagom test cases and the kernel, respectively.
- `ENABLE_MEMORY_SANITIZER`: enables runtime checks for uninitialized memory accesses in Lagom test cases.
- `ENABLE_UNDEFINED_SANITIZER`: builds in runtime checks for [undefined behavior](https://en.wikipedia.org/wiki/Undefined_behavior) (like null pointer dereferences and signed integer overflows) in Lagom test cases.
- `ENABLE_FUZZER_SANITIZER`: builds [fuzzers](https://en.wikipedia.org/wiki/Fuzzing) for various parts of the system.
- `ENABLE_EXTRA_KERNEL_DEBUG_SYMBOLS`: sets -Og and -ggdb3 compile options for building the Kernel. Allows for easier debugging of Kernel code. By default, the Kernel is built with -Os instead.
- `ENABLE_ALL_THE_DEBUG_MACROS`: used for checking whether debug code compiles on CI. This should not be set normally, as it clutters the console output and makes the system run very slowly. Instead, enable only the needed debug macros, as described below.
- `ENABLE_ALL_DEBUG_FACILITIES`: used for checking whether debug code compiles on CI. Enables both `ENABLE_ALL_THE_DEBUG_MACROS` and `ENABLE_EXTRA_KERNEL_DEBUG_SYMBOLS`.
- `ENABLE_COMPILETIME_FORMAT_CHECK`: checks for the validity of `std::format`-style format string during compilation. Enabled by default.
- `ENABLE_PCI_IDS_DOWNLOAD`: downloads the [`pci.ids` database](https://pci-ids.ucw.cz/) that contains information about PCI devices at build time, if not already present. Enabled by default.
- `BUILD_LAGOM`: builds [Lagom](../Meta/Lagom/ReadMe.md), which makes various SerenityOS libraries and programs available on the host system.
- `PRECOMPILE_COMMON_HEADERS`: precompiles some common headers to speedup compilation.
- `ENABLE_KERNEL_LTO`: builds the kernel with link-time optimization.
- `INCLUDE_WASM_SPEC_TESTS`: downloads and includes the WebAssembly spec testsuite tests

Many parts of the SerenityOS codebase have debug functionality, mostly consisting of additional messages printed to the debug console. This is done via the `<component_name>_DEBUG` macros, which can be enabled individually at build time. They are listed in [this file](../Meta/CMake/all_the_debug_macros.cmake).

To toggle a build option, add it to the `cmake` command invocation with a `-D` prefix. To enable it, add `=ON` at the end, or add `=OFF` to disable it. The complete command should look similarly to this:

```console
$ cmake ../.. -G Ninja -DPROCESS_DEBUG=ON -DENABLE_PCI_IDS_DOWNLOAD=OFF
```

For the changes to take effect, SerenityOS needs to be recompiled and the disk image needs to be rebuilt.

## Ports

To add a package from the ports collection to Serenity, for example curl, go into `Ports/curl/` and run `./package.sh`. The sourcecode for the package will be downloaded and the package will be built. After that, rebuild the disk image. The next time you start Serenity, `curl` will be available.

## Tests

For information on running host and target tests, see [Running Tests](RunningTests.md). The documentation there explains the difference between host tests run with Lagom and
target tests run on SerenityOS. It also contains useful information for debugging CI test failures.

## Customize disk image

To add, modify or remove files of the disk image's file system, e.g. to change the default keyboard layout, you can create a shell script with the name `sync-local.sh` in the project root, with content like this:

```sh
#!/bin/sh

set -e

cat << 'EOF' > mnt/etc/Keyboard.ini
[Mapping]
Keymap=de
EOF
```

This will configure your keymap to German (`de`) instead of US English. See [`Base/res/keymaps/`](../Base/res/keymaps/) for a full list. Note that the `keymap` program itself will also modify the `/etc/Keyboard.ini` config file, but this way the change will persist across image rebuilds.
