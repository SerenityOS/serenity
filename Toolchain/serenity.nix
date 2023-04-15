{ pkgs ? import <nixpkgs> { } }:
with pkgs;

stdenv.mkDerivation {
  name = "cpp-env";
  nativeBuildInputs = [
    gcc12
    curl
    cmake
    mpfr
    ninja
    gmp
    libmpc
    e2fsprogs
    fuse2fs
    patch
    ccache
    rsync
    unzip
    texinfo
    pkg-config
    # To create port launcher icons
    imagemagick
  ];
  buildInputs = [
    openssl
    libxcrypt
    xlibsWrapper
    qemu
    e2fsprogs
    fuse2fs
    # To build the GRUB disk image
    grub2
    parted
  ];

  hardeningDisable = [ "format" "fortify" ];
}
