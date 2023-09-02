#!/usr/bin/env -S bash ../.port_include.sh
port=wireguard-tools
version=1.0.20210914
files=(
    "https://git.zx2c4.com/wireguard-tools/snapshot/wireguard-tools-${version}.tar.xz#97ff31489217bb265b7ae850d3d0f335ab07d2652ba1feec88b734bc96bd05ac"
)
makeopts=("--directory=src")
# NOTE: PREFIX extends DESTDIR and therefore must not include $SERENITY_INSTALL_ROOT!
installopts=("--directory=src" "PREFIX=/usr/local")
