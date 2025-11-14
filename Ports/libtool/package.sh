#!/usr/bin/env -S bash ../.port_include.sh
port='libtool'
version='2.5.4'
useconfigure='true'
depends=(
    'bash'
    'sed'
)
files=(
    "https://ftpmirror.gnu.org/gnu/libtool/libtool-${version}.tar.xz#f81f5860666b0bc7d84baddefa60d1cb9fa6fceb2398cc3baca6afaa60266675"
)
configopts=("--prefix=/usr/local")

post_install() {
    mkdir -p "${SERENITY_INSTALL_ROOT}/usr/bin"
    ln -sf /usr/local/bin/sed "${SERENITY_INSTALL_ROOT}/usr/bin/sed"
}
