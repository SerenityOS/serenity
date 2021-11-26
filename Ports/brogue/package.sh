#!/usr/bin/env -S bash ../.port_include.sh
port=brogue
depends=("SDL2" "SDL2_image")
version=1.9.3
workdir="BrogueCE-${version}"
files="https://github.com/tmewett/BrogueCE/archive/refs/tags/v${version}.tar.gz brogue.tar.gz 441182916a16114bedfee614b09a198b4877a25db2544c5e087c86038aae2452"
auth_type=sha256
makeopts=("bin/brogue")

install() {
    datadir="$SERENITY_INSTALL_ROOT/usr/local/share/games/brogue/assets"
    mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    mkdir -p $datadir
    cp $workdir/bin/assets/* $datadir
    cp "${workdir}/bin/brogue" "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
