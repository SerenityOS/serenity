#!/usr/bin/env -S bash ../.port_include.sh
port=libmikmod
version=3.3.11.1
useconfigure=true
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
use_fresh_config_sub=true
config_sub_paths=("autotools/config.sub")
files=(
    "https://downloads.sourceforge.net/project/mikmod/libmikmod/${version}/libmikmod-${version}.tar.gz ad9d64dfc8f83684876419ea7cd4ff4a41d8bcd8c23ef37ecb3a200a16b46d19"
)

configure() {
    run cmake "${configopts[@]}" .
}

install() {
    run make install
}
