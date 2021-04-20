#!/usr/bin/env -S bash ../.port_include.sh
port=ninja
version=1.8.2
files="https://github.com/ninja-build/ninja/archive/v${version}.tar.gz ninja-v${version}.tar.gz 5fdb04461cc7f5d02536b3bfc0300166"
auth_type=md5

build() {
    CXXFLAGS="--sysroot=${SERENITY_INSTALL_ROOT}" \
    LDFLAGS="--sysroot=${SERENITY_INSTALL_ROOT}" \
    # platform=linux is close enough.
    run ./configure.py --bootstrap --platform=linux
    run strip ninja
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp ninja "${SERENITY_INSTALL_ROOT}/usr/local/bin/ninja"
}
