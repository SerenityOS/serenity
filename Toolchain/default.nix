{ pkgs ? import <nixpkgs> { } }: with pkgs;

mkShell.override { stdenv = gcc13Stdenv; } {
  packages = [
    ccache
    cmake
    curl
    e2fsprogs
    fuse2fs
    gcc13
    gmp
    # To create port launcher icons
    imagemagick
    libmpc
    mpfr
    ninja
    patch
    pkg-config
    rsync
    texinfo
    unzip
    # To build the GRUB disk image
    grub2
    libxcrypt
    openssl
    parted
    qemu
    python3
  ];
}
