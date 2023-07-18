#!/usr/bin/env -S bash ../.port_include.sh
port='curl'
version='8.2.0'
useconfigure='true'
files="https://curl.se/download/curl-${version}.tar.bz2 curl-${version}.tar.bz2 080aaa5bef29ab3f592101e7a95f32ddbe88b92125cb28dde479d5a104928ea4"
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
    -DCURL_CA_BUNDLE=/etc/ssl/certs/ca-certificates.crt \
    -DCURL_CA_PATH=none \
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
