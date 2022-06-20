#!/usr/bin/env -S bash ../.port_include.sh
port='nano'
version='6.3'
files="https://www.nano-editor.org/dist/v6/nano-${version}.tar.xz nano-${version}.tar.xz eb532da4985672730b500f685dbaab885a466d08fbbf7415832b95805e6f8687"
auth_type='sha256'
useconfigure='true'
use_fresh_config_sub='true'
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--disable-browser" "--disable-utf8")
depends=("ncurses")

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"
