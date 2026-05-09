#!/usr/bin/env -S bash ../.port_include.sh
port='nano'
version='9.0'
files=(
    "https://www.nano-editor.org/dist/v9/nano-${version}.tar.xz#9f384374b496110a25b73ad5a5febb384783c6e3188b37063f677ac908013fde"
)
useconfigure='true'
use_fresh_config_sub='true'
configopts=("--disable-utf8")
depends=("ncurses")

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"
