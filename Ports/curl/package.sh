#!/usr/bin/env -S bash ../.port_include.sh
port='curl'
version='8.2.1'
useconfigure='true'
files=(
    "https://curl.se/download/curl-${version}.tar.bz2#0f1e31ebe336c09ec66381f1532f8350e466e1d02ffe10c4ac44a867f1b9d343"
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
