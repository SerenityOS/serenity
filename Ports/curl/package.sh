#!/usr/bin/env -S bash ../.port_include.sh
port='curl'
version='7.88.1'
useconfigure='true'
files="https://curl.se/download/curl-${version}.tar.bz2 curl-${version}.tar.bz2 8224b45cce12abde039c12dc0711b7ea85b104b9ad534d6e4c5b4e188a61c907"
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
