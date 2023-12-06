#!/usr/bin/env -S bash ../.port_include.sh
port='curl'
version='8.5.0'
useconfigure='true'
files=(
    "https://curl.se/download/curl-${version}.tar.bz2#ce4b6a6655431147624aaf582632a36fe1ade262d5fab385c60f78942dd8d87b"
)
depends=(
    'ca-certificates'
    'openssl'
    'zlib'
    'zstd'
)

configure() {
    mkdir -p curl-build
    cmake \
        -G Ninja \
        -S "curl-${version}" \
        -B curl-build \
        -DCMAKE_TOOLCHAIN_FILE="${SERENITY_BUILD_DIR}/CMakeToolchain.txt" \
        -DCURL_USE_OPENSSL='ON' \
        -DCURL_ZSTD='ON' \
        -DCURL_CA_BUNDLE='/etc/ssl/certs/ca-certificates.crt' \
        -DCURL_CA_PATH='none' \
        -DCURL_DISABLE_NTLM='ON' \
        -DCURL_DISABLE_SOCKETPAIR='ON' \
        -DCURL_DISABLE_TESTS='ON' \
        -DCURL_HIDDEN_SYMBOLS='OFF'
}

build() {
    ninja -C curl-build
}

install() {
    ninja -C curl-build install
}
