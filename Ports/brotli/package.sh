#!/usr/bin/env -S bash ../.port_include.sh

port='brotli'
version='1.2.0'
files=(
    "https://github.com/google/brotli/archive/refs/tags/v${version}.tar.gz#816c96e8e8f193b40151dad7e8ff37b1221d019dbcb9c35cd3fadbfe6477dfec"
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_INSTALL_MANDIR=${SERENITY_INSTALL_ROOT}/usr/local/share/man"
    "-DCMAKE_BUILD_TYPE=Release"
)
useconfigure='true'

configure() {
    run cmake "${configopts[@]}" .
}

install() {
    run make "${installopts[@]}" install
}
