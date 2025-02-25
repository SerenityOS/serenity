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
      # For building and installing ports
      autoconf
      automake
      gperf
      imagemagick
      libtool
      # For development
      clang-tools
      nodePackages.prettier
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
    # The toolchain is built with `mtune=native` which nix warns about, but we don't care about that warning.
    export NIX_ENFORCE_NO_NATIVE=0
  '';

  hardeningDisable = [ "format" ];
}
