#!/usr/bin/env -S bash ../.port_include.sh
port='ninja'
version='1.12.1'
files=(
    "https://github.com/ninja-build/ninja/archive/v${version}.tar.gz#821bdff48a3f683bc4bb3b6f0b5fe7b2d647cf65d52aeb63328c91a6c6df285a"
)

build() {
    CXXFLAGS="--sysroot=${SERENITY_INSTALL_ROOT}" \
    LDFLAGS="--sysroot=${SERENITY_INSTALL_ROOT}" \
    # platform=linux is close enough.
    run ./configure.py --bootstrap --platform='linux'
    run strip ninja
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp ninja "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
