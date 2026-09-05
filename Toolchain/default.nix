{
  pkgs ? import <nixpkgs> { },
}:
with pkgs;

mkShell.override { stdenv = gccStdenv; } {
  packages = [
    ccache
    cmake
    curl
    e2fsprogs
    flex
    git
    gmp
    libmpc
    libxcrypt
    mpfr
    ninja
    openssl
    patch
    pkg-config
    python3
    qemu
    rsync
    texinfo
    unzip
    wget
    # For building Jakt
    llvmPackages_20.llvm
    # For building and installing ports
    autoconf
    automake
    gperf
    imagemagick
    libtool
    # Must be installed for LPeg and luarocks, same version as
    # the Lua port, ends up in PATH as 'lua'
    lua5_5
    # Must be installed for LPeg but not needed directly
    lua5_1
    luarocks
    lzip
    meson
    nasm
    perl
    tcl-9_0
    wayland-scanner
    # For development
    # NOTE: The unwrapped clang package is used because the one installed by `clang-tools`
    #       adds extra include and resource directories that conflict with serenity's custom toolchain.
    # FIXME: Go back to the `clang-tools` package once https://github.com/NixOS/nixpkgs/pull/354755 is merged.
    llvmPackages_20.clang-unwrapped
    prettier
    pre-commit
  ]
  ++ lib.optionals stdenv.hostPlatform.isLinux [
    fuse2fs
    grub2
    parted
  ]
  ++ lib.optionals stdenv.hostPlatform.isDarwin [
    genext2fs
  ];

  buildInputs = [
    # Declared as a build input to be available to pkg-config,
    # used by sgt-puzzles to build icons on the host
    gtk3
  ] ++ lib.optionals stdenv.hostPlatform.isDarwin [
    apple-sdk_13
    (darwinMinVersionHook "13.3")
  ];

  # The toolchain is built with `mtune=native` which nix warns about, but we don't care about that warning.
  NIX_ENFORCE_NO_NATIVE = 0;

  hardeningDisable = [ "format" ];
}
