#!/usr/bin/env -S bash ../.port_include.sh
port='curl'
version='8.4.0'
useconfigure='true'
files=(
    "https://curl.se/download/curl-${version}.tar.bz2#e5250581a9c032b1b6ed3cf2f9c114c811fc41881069e9892d115cc73f9e88c6"
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
        -DCURL_HIDDEN_SYMBOLS='OFF' \
        -DHAVE_GETADDRINFO_THREADSAFE=1
}

build() {
    ninja -C curl-build
}

install() {
    ninja -C curl-build install
}
