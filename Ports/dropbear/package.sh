#!/usr/bin/env -S bash ../.port_include.sh
port='dropbear'
version='2026.94'
files=(
    "https://mirror.dropbear.nl/mirror/releases/dropbear-${version}.tar.bz2#e098034a843699200c8c977a991fff73159735bf795d5f72ef672c41a6b1ae81"
)
useconfigure='true'
depends=(
    'zlib'
)
configopts=(
    # Serenity's utmp is not fully compatible with what dropbear expects.
    '--disable-utmp'
    # Serenity doesn't have lastlog.
    '--disable-lastlog'
    # dropbear installs to sbin by default; we want it in bin.
    '--sbindir=/usr/local/bin'
)
