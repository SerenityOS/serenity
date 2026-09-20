#!/usr/bin/env -S bash ../.port_include.sh
port='libmikmod'
version='3.3.14'
useconfigure='true'
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
files=(
    "https://downloads.sourceforge.net/project/mikmod/libmikmod/${version}/libmikmod-${version}.tar.gz#dffd82b8f254c3489c32098da831f33eac7136843d1e7ccb802f1254ad5b4219"
)

configure() {
    run cmake "${configopts[@]}" .
}

install() {
    run make install
}
