#!/usr/bin/env -S bash ../.port_include.sh
port='brogue'
depends=(
    'SDL2'
    'SDL2_image'
)
version='1.15.1'
workdir="BrogueCE-${version}"
files=(
    "https://github.com/tmewett/BrogueCE/archive/refs/tags/v${version}.tar.gz#2abc186c5327342cb9ad7e45d41096ab10797d5ba76dcac843824ac2a0bfb3ac"
)
makeopts+=(
    'bin/brogue'
)
launcher_name='Brogue'
launcher_category='&Games'
launcher_command='/usr/local/bin/brogue'
icon_file='bin/assets/icon.png'

install() {
    datadir="${SERENITY_INSTALL_ROOT}/usr/local/share/games/brogue/assets"
    mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    mkdir -p "${datadir}"
    cp "${workdir}"/bin/assets/* "${datadir}"
    cp "${workdir}/bin/brogue" "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
