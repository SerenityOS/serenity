#!/usr/bin/env -S bash ../.port_include.sh
port='curl'
version='7.87.0'
files="https://curl.se/download/curl-${version}.tar.bz2 curl-${version}.tar.bz2 5d6e128761b7110946d1276aff6f0f266f2b726f5e619f7e0a057a474155f307"
auth_type='sha256'
useconfigure='true'
depends=(
    'ca-certificates'
    'openssl'
    'zlib'
    'zstd'
)
configopts=(
    "-S curl-${version}"
    '-B curl-build'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCURL_USE_OPENSSL=ON'
    "-DOPENSSL_ROOT_DIR=/usr/local/ssl"
    "-DOPENSSL_LIBRARIES=/usr/local/ssl/include"
    '-DCURL_CA_BUNDLE=/etc/ssl/certs/ca-certificates.crt'
    '-DCURL_CA_PATH=none'
    '-DCURL_ZSTD=ON'
    '-DCURL_DISABLE_NTLM=ON'
    '-DCURL_DISABLE_SOCKETPAIR=ON'
    '-DCURL_DISABLE_TESTS=ON'
    '-DCURL_HIDDEN_SYMBOLS=OFF'
)

configure() {
    mkdir -p 'curl-build'
    cmake -G Ninja "${configopts[@]}"
}

build() {
    ninja -C curl-build
}

install() {
    ninja -C curl-build install
}
