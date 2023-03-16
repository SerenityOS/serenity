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
    # Example Build-time Additional Dependencies
    pkg-config
  ];
  buildInputs = [
    # Example Run-time Additional Dependencies
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
