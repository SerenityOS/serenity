#!/usr/bin/env -S bash ../.port_include.sh
port='libssh2'
version='1.11.1'
useconfigure='true'
files=(
    "https://www.libssh2.org/download/libssh2-${version}.tar.gz#d9ec76cbe34db98eec3539fe2c899d26b0c837cb3eb466a56b0f109cabf658f7"
)
depends=(
    'openssl'
    'zlib'
)

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"

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
