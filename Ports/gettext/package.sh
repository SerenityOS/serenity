#!/usr/bin/env -S bash ../.port_include.sh
port='gettext'
version='0.26'
useconfigure='true'
files=(
    "https://ftpmirror.gnu.org/gettext/gettext-${version}.tar.gz#39acf4b0371e9b110b60005562aace5b3631fed9b1bb9ecccfc7f56e58bb1d7f"
)
depends=(
    'libiconv'
)
configopts=(
    '--disable-curses'
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
