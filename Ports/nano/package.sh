#!/usr/bin/env -S bash ../.port_include.sh
port=nano
version=6.0
useconfigure="true"
use_fresh_config_sub=true
files="https://www.nano-editor.org/dist/v6/nano-${version}.tar.xz nano-${version}.tar.xz
https://www.nano-editor.org/dist/v6/nano-${version}.tar.xz.asc nano-${version}.tar.xz.asc"
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--disable-browser" "--disable-utf8")
depends=("ncurses")
auth_type="sig"
auth_import_key="BFD009061E535052AD0DF2150D28D4D2A0ACE884"
auth_opts=("nano-${version}.tar.xz.asc" "nano-${version}.tar.xz")

export CPPFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses"
export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"
