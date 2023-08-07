#!/usr/bin/env -S bash ../.port_include.sh
port='brogue'
depends=("SDL2" "SDL2_image")
version='1.11.1'
workdir="BrogueCE-${version}"
files=(
    "https://github.com/tmewett/BrogueCE/archive/refs/tags/v${version}.tar.gz v${version}.tar.gz dc562cf774f88b12b6aeebdac5a00e62e8598b3f84da2130a54a67a60c5debf2"
)
makeopts+=("bin/brogue")

install() {
    datadir="${SERENITY_INSTALL_ROOT}/usr/local/share/games/brogue/assets"
    mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    mkdir -p "${datadir}"
    cp "${workdir}"/bin/assets/* "${datadir}"
    cp "${workdir}/bin/brogue" "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
