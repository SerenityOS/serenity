{
  pkgs ? import <nixpkgs> { },
}:
with pkgs;

mkShell.override { stdenv = gccStdenv; } {
  packages =
    [
      ccache
      cmake
      curl
      e2fsprogs
      gmp
      libmpc
      mpfr
      ninja
      patch
      pkg-config
      rsync
      texinfo
      unzip
      wget
      libxcrypt
      openssl
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
    ]
    ++ lib.optionals stdenv.isLinux [
      fuse
      fuse-ext2
      grub2
      parted
    ]
    ++ lib.optionals stdenv.isDarwin [
      genext2fs
    ];

  buildInputs = lib.optionals stdenv.isDarwin [
    apple-sdk_13
    (darwinMinVersionHook "13.3")
  ];

  shellHook = ''
    # The toolchain is built with `mtune=native` which nix warns about but we don't care about that warning, so just disable it.
    export NIX_ENFORCE_NO_NATIVE=0
  '';

  hardeningDisable = [ "format" ];
}
