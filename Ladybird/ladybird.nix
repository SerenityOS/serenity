{ pkgs ? import <nixpkgs> { } }:
pkgs.mkShell.override
{
  stdenv = pkgs.gcc12Stdenv;
}
{
  name = "ladybird";

  nativeBuildInputs = with pkgs; [
    pkgconfig
    cmake
    ninja
    qt6.qtbase
    qt6.qtbase.dev
    qt6.qttools
    qt6.qtwayland
    qt6.qtwayland.dev
  ];

  shellHook = ''
    export QT_QPA_PLATFORM="wayland;xcb"
  '';
}
