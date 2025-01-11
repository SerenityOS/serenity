#!/usr/bin/env -S bash ../.port_include.sh
port=libmikmod
version=3.3.12
useconfigure=true
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
use_fresh_config_sub=true
config_sub_paths=("autotools/config.sub")
files=(
    "https://downloads.sourceforge.net/project/mikmod/libmikmod/${version}/libmikmod-${version}.tar.gz#adef6214863516a4a5b44ebf2c71ef84ecdfeb3444973dacbac70911c9bc67e9"
)

configure() {
    run cmake "${configopts[@]}" .
}

install() {
    run make install
}
