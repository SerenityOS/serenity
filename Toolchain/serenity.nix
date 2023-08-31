{ pkgs ? import <nixpkgs> { } }:
with pkgs;

stdenv.mkDerivation {
  name = "cpp-env";
  nativeBuildInputs = [
    ccache
    cmake
    curl
    e2fsprogs
    fuse2fs
    gcc12
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
  ];

  buildInputs = [
    e2fsprogs
    fuse2fs
    # To build the GRUB disk image
    grub2
    libxcrypt
    openssl
    parted
    qemu
    python3
  ];

  hardeningDisable = [ "format" "fortify" ];
}
