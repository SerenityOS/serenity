#!/usr/bin/env -S bash ../.port_include.sh
port=libssh2
version=1.10.0
useconfigure=true
files=(
    "https://www.libssh2.org/download/libssh2-${version}.tar.gz libssh2-${version}.tar.gz 2d64e90f3ded394b91d3a2e774ca203a4179f69aebee03003e5a6fa621e41d51"
)
depends=("libgcrypt")
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-GNinja")

configure() {
    run cmake "${configopts[@]}" .
}

build() {
    run ninja
}

install() {
    run ninja install
}
