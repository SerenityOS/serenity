#!/usr/bin/env -S bash ../.port_include.sh
port='gn'
version='2023.07.12'
useconfigure='true'
files=(
    'git+https://gn.googlesource.com/gn#fae280eabe5d31accc53100137459ece19a7a295'
)
depends=(
    'ninja'
    'python3'
)

configure() {
    run python3 build/gen.py --platform='serenity' --allow-warnings
}

build() {
    run ninja -C out
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp out/gn "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp out/gn_unittests "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
