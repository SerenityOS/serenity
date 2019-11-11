# Serenity Toolchain - Building the Serenity operating system

This directory contains all toolchain related files. E.g. build scripts for
the cross compilation toolchain and build toolchain for ports.

- [Serenity Toolchain - Building the Serenity operating system](#serenity-toolchain---building-the-serenity-operating-system)
  - [Cross Compile Toolchain](#cross-compile-toolchain)
    - [Dependencies](#dependencies)
  - [Serenity (Full build)](#serenity-full-build)
  - [Running SerenityOS in an emulator](#running-serenityos-in-an-emulator)
    - [QEMU installation / compilation](#qemu-installation--compilation)
    - [Passing custom arguments to QEMU](#passing-custom-arguments-to-qemu)

## Cross Compile Toolchain

The cross compile toolchain contains

- binutils 2.32

- GCC 8.3.0

These are built from source with some patches applied.

### Dependencies

- Build Essentials

    ```bash
    sudo apt install build-essential curl libmpfr-dev libmpc-dev libgmp-dev
    ```

- GCC 8

    ```bash
    sudo add-apt-repository ppa:ubuntu-toolchain-r/test
    sudo apt-get install gcc-8 g++-8
    sudo update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-8 800 --slave /usr/bin/g++ g++ /usr/bin/g++-8
    ```

- e2fsprogs

    ```bash
    sudo apt install e2fsprogs
    ```

## Serenity (Full build)

If everything worked out, you now have the **i686-pc-serenity** toolchain ready and we can build Serenity.

Go into `Kernel/` folder and build it:

```bash
./makeall.sh
```

Then take it for a spin:

```bash
./run
```

See next chapter for more options on running SerenityOS in an emulator.

## Running SerenityOS in an emulator

To run SerenityOS in a specific emulator, call the `./run` command in the `Kernel/` folder:

```bash
./run
```

There are several emulators supported to run SerenityOS in:

- Bochs

    ```bash
    sudo apt install bochs
    ```

    Add the `b` argument to the run script, to use bochs emulator:

    ```bash
    ./run b
    ```

- QEMU
    QEMU with networking enabled is used by default, when no extra argument is passed to the run script.
    There are some extra arguments to run QEMU emulator with specific settings:

    Add the `qn` argument to the run script to use QEMU without networking:

    ```bash
    ./run qn
    ```

    Add the `qgrub` argument to the run script to use QEMU with grub bootloader:

    ```bash
    ./run qgrub
    ```

    Add the `qtext` argument to the run script to use QEMU with textmode:

    ```bash
    ./run qtext
    ```

    Note: there is a problem with the PS/2 keyboard/mouse emulation in QEMU 2.11.1 as packaged in Ubuntu's LTS releases.
    If you have any strange behaviour with missing keyboard inputs or jittery mouse movement, try building QEMU from
    source as described in [QEMU](#qemu-installation--compilation). 2.12.1, 3.0.1, 3.1.0, and 4.0.0 are all confirmed as working when built from source.

### QEMU installation / compilation

If your distribution contains a QEMU version > 2.11.1, then you can just install it via

```bash
sudo apt install qemu-system-i386 qemu-utils
```

If that is not the case, you can build QEMU from sources with the provided script `BuildQemu.sh`.
To do so, some build dependencies have to be installed first:

```bash
sudo apt-get build-dep qemu
sudo apt-get install libgtk-3-dev
```

The source-repositories of your distribution have to be enabled to install the build-dep's.

`BuildQemu.sh` has been tested with QEMU 3.0.0 and 4.1.0 (which is default). If you
want to build QEMU 3.0.0, change the variable `QEMU_VERSION` and `QEMU_MD5SUM` accordingly:

```bash
QEMU_VERSION="qemu-3.0.0"
QEMU_MD5SUM="${QEMU300_MD5SUM}"
```

### Passing custom arguments to QEMU

You can modify the environment variable `SERENITY_EXTRA_QEMU_ARGS` to your needs or hand it over directly before the run command:

```bash
SERENITY_EXTRA_QEMU_ARGS="-nographic" ./run qtext
```
