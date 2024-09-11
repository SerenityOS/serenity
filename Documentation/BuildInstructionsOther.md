# Installing build requisites on other systems

### Fedora

```console
sudo dnf install texinfo binutils-devel curl cmake mpfr-devel libmpc-devel gmp-devel e2fsprogs ninja-build patch ccache rsync @"C Development Tools and Libraries" @Virtualization
```

Optional: `e2fsprogs` package for [building images without root](https://github.com/SerenityOS/serenity/pull/11224).

## openSUSE

```console
sudo zypper install curl cmake mpfr-devel mpc-devel ninja gmp-devel e2fsprogs patch qemu-x86 qemu-audio-pa gcc gcc-c++ ccache rsync patterns-devel-C-C++-devel_C_C++
```

## Void Linux

```console
sudo xbps-install -S base-devel cmake curl mpfr-devel libmpc-devel gmp-devel e2fsprogs ninja qemu ccache rsync
```

## ALT Linux

```console
apt-get install curl cmake libmpc-devel gmp-devel e2fsprogs libmpfr-devel ninja-build patch gcc ccache rsync
```

## NixOS

You can use the flake in the root directory to enter a devShell that has all the required packages and tools to build SerenityOS:

```console
nix develop
```

Or you can use the legacy `nix-shell` tool to enter the devShell:

```console
nix-shell Toolchain
```

This will use the `Toolchain/default.nix` file and your host `nixpkgs`.

## Alpine Linux

First, make sure you have enabled the `community` repository in `/etc/apk/repositories` and run `apk update`. It has been tested on `edge`, YMMV on `stable`.

```console
# the basics, if you have not already done so
apk add bash curl git util-linux sudo

# GNU coreutils for GNU's version of `du`
apk add coreutils

# rough equivalent of build-essential
apk add build-base

# qemu
apk add qemu qemu-system-x86_64 qemu-img qemu-ui-gtk qemu-audio-pa

# build tools (samurai is a drop-in replacement for ninja)
apk add cmake e2fsprogs grub-bios samurai mpc1-dev mpfr-dev gmp-dev ccache rsync texinfo
```

Optional: `fuse2fs` for [building images without root](https://github.com/SerenityOS/serenity/pull/11224).

## OpenBSD prerequisites

```console
doas pkg_add bash cmake g++ gcc git gmake gmp ninja ccache rsync coreutils qemu sudo e2fsprogs
```

## FreeBSD prerequisites

```console
pkg install qemu bash cmake coreutils e2fsprogs fusefs-ext2 gcc11 git gmake ninja sudo gmp mpc mpfr ccache rsync
```

Optional: `fusefs-ext2` for [building images without root](https://github.com/SerenityOS/serenity/pull/11224).
