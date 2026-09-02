#!/usr/bin/env -S bash ../.port_include.sh
port='nano'
version='9.2'
files=(
    "https://www.nano-editor.org/dist/v9/nano-${version}.tar.xz#05ecb99247b782e8a5b3a25ed4101dd034b0236902f7449bc9795b717642f7e9"
)
useconfigure='true'
configopts=("--disable-utf8")
depends=("ncurses")

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"
