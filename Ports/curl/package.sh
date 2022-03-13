#!/usr/bin/env -S bash ../.port_include.sh
port=curl
version=7.82.0
useconfigure=true
files="https://curl.se/download/curl-${version}.tar.bz2 curl-${version}.tar.bz2 46d9a0400a33408fd992770b04a44a7434b3036f2e8089ac28b57573d59d371f"
auth_type=sha256
depends=("ca-certificates" "openssl" "zlib" "zstd")
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
