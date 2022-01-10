#!/usr/bin/env -S bash ../.port_include.sh
port=ccache
version=4.5.1
useconfigure=true
files="https://github.com/ccache/ccache/releases/download/v${version}/ccache-${version}.tar.gz ccache-$version.tar.gz f0d3cff5d555d6868f14a7d05696f0370074e475304fd5aa152b98f892364981"
auth_type=sha256
depends=("zstd")
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-DCMAKE_BUILD_TYPE=Release" "-DREDIS_STORAGE_BACKEND=OFF" "-GNinja")

configure() {
    run cmake "${configopts[@]}" .
}

build() {
    run ninja
}

install() {
    run ninja install
}
