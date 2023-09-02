#!/usr/bin/env -S bash ../.port_include.sh
port='flare-game'
useconfigure='true'
version='1.14'
depends=(
    'flare-engine'
)
files=(
    "https://github.com/flareteam/flare-game/archive/refs/tags/v${version}.tar.gz#65758462070aa88842084f8ee69083d8226e93cfbf83481663276d8307494b8e"
)

configopts=(
    "-DCMAKE_INSTALL_PREFIX=${SERENITY_INSTALL_ROOT}/usr/local"
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make "${installopts[@]}" install
}
