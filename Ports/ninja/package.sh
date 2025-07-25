#!/usr/bin/env -S bash ../.port_include.sh
port='ninja'
version='1.13.1'
files=(
    "https://github.com/ninja-build/ninja/archive/v${version}.tar.gz#f0055ad0369bf2e372955ba55128d000cfcc21777057806015b45e4accbebf23"
)

build() {
    CXXFLAGS="--sysroot=${SERENITY_INSTALL_ROOT}" \
    LDFLAGS="--sysroot=${SERENITY_INSTALL_ROOT}" \
    # platform=linux is close enough.
    run ./configure.py --bootstrap --platform='linux'
    run "$STRIP" ninja
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp ninja "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
