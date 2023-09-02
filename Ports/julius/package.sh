#!/usr/bin/env -S bash ../.port_include.sh
port='julius'
version='1.7.0'
useconfigure='true'
files=(
    "https://github.com/bvschaik/julius/archive/refs/tags/v${version}.tar.gz#3ee62699bcbf6c74fe5a9c940c62187141422a9bd98e01747a554fd77483431f"
)
depends=(
    'libpng'
    'SDL2'
    'SDL2_mixer'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)

configure() {
    run cmake "${configopts[@]}" .
}

install() {
    run make "${installopts[@]}" install
}
