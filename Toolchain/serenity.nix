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
    # e2fsprogs needs some optional parameter to activate fuse2fs with which
    # the qemu image will be mounted without root access.
    (e2fsprogs.overrideAttrs (oldAttrs: {
      buildInputs = oldAttrs.buildInputs ++ [ pkgs.fuse ];
    }))
    # glibc
  ];
  
  hardeningDisable = [ "format" "fortify" ];
}
