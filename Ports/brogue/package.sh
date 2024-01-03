#!/usr/bin/env -S bash ../.port_include.sh
port='brogue'
depends=(
    'SDL2'
    'SDL2_image'
)
version='1.12'
workdir="BrogueCE-${version}"
files=(
    "https://github.com/tmewett/BrogueCE/archive/refs/tags/v${version}.tar.gz#aeed3f6ca0f4e352137b0196e9dddbdce542a9e99dda9effd915e018923cd428"
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
