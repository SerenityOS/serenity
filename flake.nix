{
  inputs = {
    nixpkgs.url = "nixpkgs/nixos-22.11";
    flake-utils.url = "github:numtide/flake-utils";
  };
  outputs = {
    nixpkgs,
    flake-utils,
    ...
  }:
    flake-utils.lib.eachDefaultSystem (
      system: let
        pkgs = import nixpkgs {
          inherit system;
        };
        nativeBuildInputs = with pkgs; [
          pkg-config
          curl
          cmake
          qemu
          ninja
          qemu-utils
          ccache
          rsync
          unzip
          texinfo
          gcc12
          e2fsprogs.fuse2fs
          genext2fs
          fuse
          autoconf
        ];
        buildInputs = with pkgs; [
          mpfr.dev
          libmpc
          gmp.dev
          libxcrypt
        ];
      in {
        formatter = pkgs.alejandra;
        devShell = pkgs.mkShell {
          inherit nativeBuildInputs buildInputs;

          # Some hardening flags currently cause the build to fail.
          NIX_HARDENING_ENABLE = "";

          # CMake fails and suggests this variable as a fix, which works.
          # I assume it's needed because NixOS doesn't have a standard FHS.
          SERENITY_CMAKE_ARGS = "-DCMAKE_BUILD_WITH_INSTALL_RPATH=true";

          shellHook = ''
            # The default fusermount does not have setuid.
            if [ -x '/run/wrappers/bin/fusermount3' ]; then
                export FUSERMOUNT_PATH='/run/wrappers/bin/fusermount3'
            fi
          '';
        };
      }
    );
}
