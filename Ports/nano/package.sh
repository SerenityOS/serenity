#!/usr/bin/env -S bash ../.port_include.sh
port=nano
version=4.5
useconfigure="true"
files="https://www.nano-editor.org/dist/v4/nano-${version}.tar.xz nano-${version}.tar.xz
https://www.nano-editor.org/dist/v4/nano-${version}.tar.xz.asc nano-${version}.tar.xz.asc"
configopts="--target=${SERENITY_ARCH}-pc-serenity --disable-browser --disable-utf8"
depends="ncurses"
auth_type="sig"
auth_import_key="BFD009061E535052AD0DF2150D28D4D2A0ACE884"
auth_opts="nano-${version}.tar.xz.asc nano-${version}.tar.xz"

export CPPFLAGS="-I${SERENITY_BUILD_DIR}/Root/usr/local/include/ncurses"
export PKG_CONFIG_PATH="${SERENITY_BUILD_DIR}/Root/usr/local/lib/pkgconfig"
