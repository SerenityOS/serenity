{ pkgs ? import <nixpkgs> { } }:
pkgs.mkShell.override
{
  stdenv = pkgs.gcc12Stdenv;
}
{
  name = "ladybird";

  nativeBuildInputs = with pkgs; [
    pkgconfig
    ccache
    cmake
    ninja
    libxcrypt
    qt6.qtbase
    qt6.qtbase.dev
    qt6.qttools
    qt6.qtwayland
    qt6.qtwayland.dev
  ];

  shellHook = ''
    # NOTE: This is required to make it find the wayland platform plugin installed
    #       above, but should probably be fixed upstream.
    export QT_PLUGIN_PATH="$QT_PLUGIN_PATH:${pkgs.qt6.qtwayland}/lib/qt-6/plugins"
    export QT_QPA_PLATFORM="wayland;xcb"
  '';
}
