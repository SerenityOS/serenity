#!/usr/bin/env -S bash ../.port_include.sh
port=ccache
version=4.6
useconfigure=true
files="https://github.com/ccache/ccache/releases/download/v${version}/ccache-${version}.tar.gz ccache-$version.tar.gz 73a1767ac6b7c0404a1a55f761a746d338e702883c7137fbf587023062258625"
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
