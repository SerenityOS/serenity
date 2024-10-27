#!/usr/bin/env -S bash ../.port_include.sh
port='nano'
version='8.2'
files=(
    "https://www.nano-editor.org/dist/v8/nano-${version}.tar.xz#d5ad07dd862facae03051c54c6535e54c7ed7407318783fcad1ad2d7076fffeb"
)
useconfigure='true'
use_fresh_config_sub='true'
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--disable-browser" "--disable-utf8")
depends=("ncurses")

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"
