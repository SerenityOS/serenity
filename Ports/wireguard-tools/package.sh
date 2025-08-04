#!/usr/bin/env -S bash ../.port_include.sh
port='wireguard-tools'
version='1.0.20250521'
files=(
    "https://git.zx2c4.com/wireguard-tools/snapshot/wireguard-tools-${version}.tar.xz#b6f2628b85b1b23cc06517ec9c74f82d52c4cdbd020f3dd2f00c972a1782950e"
)
makeopts=(
    '--directory=src'
)
installopts=(
    '--directory=src'
    # NOTE: PREFIX extends DESTDIR and therefore must not include $SERENITY_INSTALL_ROOT!
    'PREFIX=/usr/local'
)
