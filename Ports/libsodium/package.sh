#!/usr/bin/env -S bash ../.port_include.sh
port='libsodium'
version='1.0.22'
useconfigure='true'
configopts=("--disable-static" "--enable-shared")
files=(
    "https://download.libsodium.org/libsodium/releases/libsodium-${version}.tar.gz#adbdd8f16149e81ac6078a03aca6fc03b592b89ef7b5ed83841c086191be3349"
)
