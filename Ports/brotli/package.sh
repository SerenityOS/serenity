#!/usr/bin/env -S bash ../.port_include.sh

port='brotli'
version='1.2.1'
files=(
    "https://github.com/google/brotli/archive/refs/tags/v${version}.tar.gz#98d35d11a78dc453aebb7d10c3748f3f73ff78c8267c0dc697b8631f67bf7e8f"
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
