{
  pkgs ? import <nixpkgs> { },
}:
with pkgs;

mkShell.override { stdenv = gcc13Stdenv; } {
  packages = [
    ccache
    cmake
    curl
    e2fsprogs
    fuse2fs
    gcc13
    gmp
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
    # For building and installing ports
    autoconf
    automake
    gperf
    imagemagick
    libtool
    # For clangd and clang-format
    clang-tools
    # For LibWeb-related formatting
    nodePackages.prettier
    # For the pre-commit hooks
    pre-commit
  ];
  hardeningDisable = [ "format" ];
}
