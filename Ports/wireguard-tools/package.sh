#!/usr/bin/env -S bash ../.port_include.sh
port='wireguard-tools'
version='1.0.20260223'
files=(
    "https://git.zx2c4.com/wireguard-tools/snapshot/wireguard-tools-${version}.tar.xz#af459827b80bfd31b83b08077f4b5843acb7d18ad9a33a2ef532d3090f291fbf"
)
makeopts=(
    '--directory=src'
)
installopts=(
    '--directory=src'
    # NOTE: PREFIX extends DESTDIR and therefore must not include $SERENITY_INSTALL_ROOT!
    'PREFIX=/usr/local'
)
