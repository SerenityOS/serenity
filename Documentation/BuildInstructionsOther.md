# Installing build requisites on other systems

### Fedora

```console
sudo dnf install binutils-devel curl cmake mpfr-devel libmpc-devel gmp-devel e2fsprogs ninja-build patch ccache rsync @"C Development Tools and Libraries" @Virtualization
```

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

You can use a `nix-shell` script like the following to set up the correct environment:

myshell.nix:

```
with import <nixpkgs> {};

stdenv.mkDerivation {
  name = "cpp-env";
  nativeBuildInputs = [
    gcc11
    curl
    cmake
    mpfr
    ninja
    gmp
    libmpc
    e2fsprogs
    patch
    ccache
    rsync
    unzip

    # Example Build-time Additional Dependencies
    pkgconfig
  ];
  buildInputs = [
    # Example Run-time Additional Dependencies
    openssl
    x11
    qemu
    # glibc
  ];
  hardeningDisable = [ "format" "fortify" ];
}
```

Then use this script: `nix-shell myshell.nix`.

Once you're in nix-shell, you should be able to follow the build directions.

## Alpine Linux

First, make sure you have enabled the `community` repository in `/etc/apk/repositories` and run `apk update`. It has been tested on `edge`, YMMV on `stable`.

```console
# the basics, if you have not already done so
apk add bash curl git util-linux sudo

# rough equivalent of build-essential
apk add build-base

# qemu
apk add qemu qemu-system-i386 qemu-img qemu-ui-gtk

# build tools (samurai is a drop-in replacement for ninja)
apk add cmake e2fsprogs grub-bios samurai mpc1-dev mpfr-dev gmp-dev ccache rsync
```

## OpenBSD prerequisites

```console
doas pkg_add bash cmake g++ gcc git gmake gmp ninja ccache rsync coreutils qemu sudo e2fsprogs
```

## FreeBSD prerequisites

```console
pkg install bash cmake coreutils e2fsprogs fusefs-ext2 gcc git gmake ninja sudo gmp mpc mpfr ccache rsync
```

