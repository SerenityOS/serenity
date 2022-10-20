{ pkgs ? import <nixpkgs> { } }:
with pkgs;

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
    texinfo
    # Example Build-time Additional Dependencies
    pkgconfig
  ];
  buildInputs = [
    # Example Run-time Additional Dependencies
    openssl
    xlibsWrapper
    qemu
    e2fsprogs
  ];

  hardeningDisable = [ "format" "fortify" ];
}
