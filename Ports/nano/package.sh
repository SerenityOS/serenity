#!/usr/bin/env -S bash ../.port_include.sh
port='nano'
version='8.5'
files=(
    "https://www.nano-editor.org/dist/v8/nano-${version}.tar.xz#000b011d339c141af9646d43288f54325ff5c6e8d39d6e482b787bbc6654c26a"
)
useconfigure='true'
use_fresh_config_sub='true'
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--disable-utf8")
depends=("ncurses")

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"
