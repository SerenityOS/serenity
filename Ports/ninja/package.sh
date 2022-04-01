#!/usr/bin/env -S bash ../.port_include.sh
port=ninja
version=1.10.2
files="https://github.com/ninja-build/ninja/archive/v${version}.tar.gz ninja-v${version}.tar.gz ce35865411f0490368a8fc383f29071de6690cbadc27704734978221f25e2bed"
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
