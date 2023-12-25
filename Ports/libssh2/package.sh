#!/usr/bin/env -S bash ../.port_include.sh
port='libssh2'
version='1.11.0'
useconfigure='true'
files=(
    "https://www.libssh2.org/download/libssh2-${version}.tar.gz#3736161e41e2693324deb38c26cfdc3efe6209d634ba4258db1cecff6a5ad461"
)
depends=(
    'openssl'
    'zlib'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" 
    '-DCRYPTO_BACKEND=OpenSSL'
    '-DENABLE_ZLIB_COMPRESSION=ON'
    '-GNinja'
)

configure() {
    run cmake "${configopts[@]}" .
}

build() {
    run ninja
}

install() {
    run ninja install
}
