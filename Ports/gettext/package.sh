#!/usr/bin/env -S bash ../.port_include.sh
port='gettext'
version='1.0'
useconfigure='true'
files=(
    "https://ftpmirror.gnu.org/gettext/gettext-${version}.tar.gz#85d99b79c981a404874c02e0342176cf75c7698e2b51fe41031cf6526d974f1a"
)
depends=(
    'libiconv'
)
configopts=(
    '--disable-curses'
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
