#!/usr/bin/env -S bash ../.port_include.sh
port='nano'
version='7.2'
files=(
    "https://www.nano-editor.org/dist/v7/nano-${version}.tar.xz nano-${version}.tar.xz 86f3442768bd2873cec693f83cdf80b4b444ad3cc14760b74361474fc87a4526"
)
useconfigure='true'
use_fresh_config_sub='true'
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--disable-browser" "--disable-utf8")
depends=("ncurses")

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"
