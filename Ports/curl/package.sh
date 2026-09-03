#!/usr/bin/env -S bash ../.port_include.sh
port='curl'
version='8.22.0'
useconfigure='true'
files=(
    "https://curl.se/download/curl-${version}.tar.bz2#5d956a6a22b3c279f50c421ee5d3c9e9d660cb6f115dcf881b579e952130549c"
)
depends=(
    'ca-certificates'
    'openssl'
    'zlib'
    'zstd'
)

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"

configure() {
    mkdir -p curl-build
    cmake \
        -G Ninja \
        -S "curl-${version}" \
        -B curl-build \
        -DCMAKE_TOOLCHAIN_FILE="${SERENITY_BUILD_DIR}/CMakeToolchain.txt" \
        -DCURL_USE_LIBPSL='OFF' \
        -DCURL_USE_OPENSSL='ON' \
        -DCURL_ZSTD='ON' \
        -DCURL_CA_BUNDLE='/etc/ssl/certs/ca-certificates.crt' \
        -DCURL_CA_PATH='none' \
        -DCURL_DISABLE_SOCKETPAIR='ON' \
        -DCURL_HIDDEN_SYMBOLS='OFF'
}

build() {
    ninja -C curl-build
}

install() {
    ninja -C curl-build install
}
