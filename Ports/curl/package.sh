#!/usr/bin/env -S bash ../.port_include.sh
port='curl'
version='7.87.0'
useconfigure='true'
files="https://curl.se/download/curl-${version}.tar.bz2 curl-${version}.tar.bz2 5d6e128761b7110946d1276aff6f0f266f2b726f5e619f7e0a057a474155f307"
auth_type='sha256'
depends=(
  'ca-certificates'
  'openssl'
  'zlib'
  'zstd'
)
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")

configure() {
    mkdir -p curl-build
    cmake -G Ninja \
    -S curl-${version} \
    -B curl-build \
    "${configopts[@]}" \
    -DCURL_USE_OPENSSL=ON \
    -DCURL_ZSTD=ON \
    -DCURL_DISABLE_NTLM=ON \
    -DCURL_DISABLE_SOCKETPAIR=ON \
    -DCURL_DISABLE_TESTS=ON \
    -DCURL_HIDDEN_SYMBOLS=OFF
}

build() {
    ninja -C curl-build
}

install() {
    ninja -C curl-build install
}
