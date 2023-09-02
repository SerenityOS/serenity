#!/usr/bin/env -S bash ../.port_include.sh
port=genemu
version=e39f690157d8f969adfbaba30a4e639d20b34768
useconfigure=true
files=(
    "https://github.com/rasky/genemu/archive/${version}.tar.gz#9b9616f6237e621a169422058caeccb2d0f4399374dc38f34837980154c89497"
)
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
depends=("SDL2")

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
