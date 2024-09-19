{
  pkgs ? import <nixpkgs> { },
}:
with pkgs;

mkShell.override { stdenv = gcc13Stdenv; } {
  packages = [
    ccache
    cmake
    libxcrypt
    ninja
    pkg-config
    qt6.qtbase
    qt6.qtbase.dev
    qt6.qtmultimedia
    qt6.qttools
    qt6.qtwayland
    qt6.qtwayland.dev
    # For clangd and clang-format
    clang-tools
    # For LibWeb-related formatting
    nodePackages.prettier
    # For the pre-commit hooks
    pre-commit
  ];

  shellHook = ''
    # NOTE: This is required to make it find the wayland platform plugin installed
    #       above, but should probably be fixed upstream.
    export QT_PLUGIN_PATH="$QT_PLUGIN_PATH:${qt6.qtwayland}/lib/qt-6/plugins"
    export QT_QPA_PLATFORM="wayland;xcb"
  '';
}
