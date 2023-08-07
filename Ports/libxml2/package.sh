#!/usr/bin/env -S bash ../.port_include.sh
port='libxml2'
version='2.9.14'
files=(
    "https://download.gnome.org/sources/libxml2/2.9/libxml2-${version}.tar.xz 60d74a257d1ccec0475e749cba2f21559e48139efba6ff28224357c7c798dfee"
)
useconfigure='true'
use_fresh_config_sub='true'
configopts=("--with-sysroot=${SERENITY_INSTALL_ROOT}" "--prefix=/usr/local" "--without-python" "--disable-static" "--enable-shared")
depends=("libiconv" "xz")
