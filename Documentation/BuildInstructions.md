# SerenityOS build instructions

## Prerequisites

Make sure you have all the dependencies installed:

### Debian / Ubuntu

```console
sudo apt install build-essential cmake curl libmpfr-dev libmpc-dev libgmp-dev e2fsprogs ninja-build qemu-system-gui qemu-system-x86 qemu-utils ccache rsync unzip texinfo
```

#### GCC 11

On Ubuntu gcc-11 is available in the repositories of 21.04 (Hirsuite) and later - add the `ubuntu-toolchain-r/test` PPA if you're running an older version:

```console
sudo add-apt-repository ppa:ubuntu-toolchain-r/test
```

Next, update your local package information from this repository:

```console
sudo apt update
```

Now on Ubuntu or Debian you can install gcc-11 with apt like this:

```console
sudo apt install gcc-11 g++-11
```

#### QEMU 5 or later

QEMU version 5 is available in Ubuntu 20.10, but it is recommended to build Qemu as provided by the toolchain by running `Toolchain/BuildQemu.sh`.
Note that you might need additional dev packages:

```console
sudo apt install libgtk-3-dev libpixman-1-dev libsdl2-dev libspice-server-dev
```

### Windows

If you're on Windows you can use WSL2 to build SerenityOS. Please have a look at the [Windows guide](BuildInstructionsWindows.md)
for details.

### Arch Linux / Manjaro

```console
sudo pacman -S --needed base-devel cmake curl mpfr libmpc gmp e2fsprogs ninja qemu qemu-arch-extra ccache rsync unzip
```

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
