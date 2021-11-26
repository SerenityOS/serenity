#!/usr/bin/env -S bash ../.port_include.sh
port=ninja
version=1.8.2
files="https://github.com/ninja-build/ninja/archive/v${version}.tar.gz ninja-v${version}.tar.gz 86b8700c3d0880c2b44c2ff67ce42774aaf8c28cbf57725cb881569288c1c6f4"
auth_type=sha256

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
