#!/usr/bin/env -S bash ../.port_include.sh
port='libxml2'
version='2.15.4'
files=(
    "https://download.gnome.org/sources/libxml2/2.15/libxml2-${version}.tar.xz#98087fd181d9070724f3fbc65c7377db03038eb92bd882374daff44940138821"
)
useconfigure='true'
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
    '--prefix=/usr/local'
    '--without-python'
    '--disable-static'
    '--enable-shared'
)
depends=(
    'libiconv'
    'xz'
)
