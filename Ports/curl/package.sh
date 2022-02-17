#!/usr/bin/env -S bash ../.port_include.sh
port=curl
version=7.81.0
useconfigure=true
files="https://curl.se/download/curl-${version}.tar.bz2 curl-${version}.tar.bz2 1e7a38d7018ec060f1f16df839854f0889e94e122c4cfa5d3a37c2dc56f1e258"
auth_type=sha256
depends=("openssl" "zlib" "zstd")
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
