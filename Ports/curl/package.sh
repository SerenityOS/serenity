#!/usr/bin/env -S bash ../.port_include.sh
port='curl'
version='8.15.0'
useconfigure='true'
files=(
    "https://curl.se/download/curl-${version}.tar.bz2#699a6d2192322792c88088576cff5fe188452e6ea71e82ca74409f07ecc62563"
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
        -DCURL_USE_LIBPSL='OFF' \
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
