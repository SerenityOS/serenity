#!/usr/bin/env -S bash ../.port_include.sh
port='curl'
version='8.0.1'
useconfigure='true'
files="https://curl.se/download/curl-${version}.tar.bz2 curl-${version}.tar.bz2 9b6b1e96b748d04b968786b6bdf407aa5c75ab53a3d37c1c8c81cdb736555ccf"
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
