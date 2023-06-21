#!/usr/bin/env -S bash ../.port_include.sh
port=libssh2
version=1.10.0
useconfigure=true
files="https://www.libssh2.org/download/libssh2-${version}.tar.gz libssh2-${version}.tar.gz
https://www.libssh2.org/download/libssh2-${version}.tar.gz.asc libssh2-${version}.tar.gz.asc"
depends=("libgcrypt")
auth_type="sig"
auth_import_key="27EDEAF22F3ABCEB50DB9A125CC908FDB71E12C2"
auth_opts=("libssh2-${version}.tar.gz.asc" "libssh2-${version}.tar.gz")
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-GNinja")

configure() {
    run cmake "${configopts[@]}" .
}

build() {
    run ninja
}

install() {
    run ninja install
}
