# SerenityOS build instructions

## Prerequisites

Make sure you have all the dependencies installed:

### Debian / Ubuntu

```console
sudo apt install build-essential cmake curl libmpfr-dev libmpc-dev libgmp-dev e2fsprogs ninja-build qemu-system-gui qemu-system-x86 qemu-utils ccache rsync unzip texinfo
```
Optional: `fuse2fs` for [building images without root](https://github.com/SerenityOS/serenity/pull/11224).

#### GCC 12

On Ubuntu gcc-12 is available in the repositories of 22.04 (Jammy) and later.
If you are running an older version, you will either need to upgrade, or find an alternative installation source.

Next, update your local package information from this repository:

```console
sudo apt update
```

Now on Ubuntu or Debian you can install gcc-12 with apt like this:

```console
sudo apt install gcc-12 g++-12
```

#### QEMU 6.2 or later

Version 6.2 of QEMU is available in Ubuntu 22.04. On earlier versions of Ubuntu,
you can build the recommended version of QEMU as provided by the toolchain by running
`Toolchain/BuildQemu.sh`.
Note that you might need additional dev packages in order to build QEMU on your machine:

```console
sudo apt install libgtk-3-dev libpixman-1-dev libsdl2-dev libspice-server-dev
```

### Windows

If you're on Windows you can use WSL2 to build SerenityOS. Please have a look at the [Windows guide](BuildInstructionsWindows.md)
for details.

### Arch Linux / Manjaro

```console
sudo pacman -S --needed base-devel cmake curl mpfr libmpc gmp e2fsprogs ninja qemu-desktop qemu-system-x86 qemu-system-aarch64 ccache rsync unzip
```
Optional: `fuse2fs` for [building images without root](https://github.com/SerenityOS/serenity/pull/11224).

### Other systems

There is also documentation for installing the build prerequisites for some less commonly used systems:

* [Other Linux distributions and \*NIX systems](BuildInstructionsOther.md)
* [macOS](BuildInstructionsMacOS.md)

## Build

In order to build SerenityOS you will first need to build the toolchain by running the following command:

```console
Meta/serenity.sh rebuild-toolchain
```

Later on, when you use `git pull` to get the latest changes, there's (usually) no need to rebuild the toolchain.

Run the following command to build and run SerenityOS:

```console
Meta/serenity.sh run
```

This will compile all of SerenityOS and install the built files into the `Build/i686/Root` directory inside your Git
repository. It will also build a disk image and start SerenityOS using QEMU.

Note that the `anon` user is able to become `root` without a password by default, as a development convenience.
To prevent this, remove `anon` from the `wheel` group and he will no longer be able to run `/bin/su`.

By default the `anon` user account's password is: `foo`

If you want to test whether your code changes compile without running the VM you can use
`Meta/serenity.sh build`. The `serenity.sh` script also provides a number of other commands. Run the script without
arguments for a list.

## Ports

To add a package from the ports collection to Serenity, for example curl, change into the `Ports/curl` directory and
run `./package.sh`. The source code for the package will be downloaded and the package will be built. The next time you
start Serenity, `curl` will be available.

## More information

At this point you should have a fully functioning VM for SerenityOS. The [advanced build instructions guide](AdvancedBuildInstructions.md)
has more information for some less commonly used features of the build system.
