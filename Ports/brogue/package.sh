#!/usr/bin/env -S bash ../.port_include.sh
port=brogue
depends=("SDL2" "SDL2_image")
version=1.10.1
workdir="BrogueCE-${version}"
files="https://github.com/tmewett/BrogueCE/archive/refs/tags/v${version}.tar.gz brogue-${version}.tar.gz 3e0425b3f1b59afe98a92c0282aa4dd7e8964b53f7cab969fcf437701a04c5fa"
auth_type=sha256
makeopts+=("bin/brogue")

install() {
    datadir="$SERENITY_INSTALL_ROOT/usr/local/share/games/brogue/assets"
    mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    mkdir -p $datadir
    cp $workdir/bin/assets/* $datadir
    cp "${workdir}/bin/brogue" "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
