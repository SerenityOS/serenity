#!/usr/bin/env -S bash ../.port_include.sh
port='libtool'
version='2.6.2'
useconfigure='true'
depends=(
    'bash'
    'sed'
)
files=(
    "mirror://gnu/libtool/libtool-${version}.tar.xz#2ef1067c16c97db930fd740cc9bc3d3ba9a583804ae5ac42cc3e8719e49e191e"
)
configopts=("--prefix=/usr/local")

post_install() {
    mkdir -p "${SERENITY_INSTALL_ROOT}/usr/bin"
    ln -sf /usr/local/bin/sed "${SERENITY_INSTALL_ROOT}/usr/bin/sed"
}
