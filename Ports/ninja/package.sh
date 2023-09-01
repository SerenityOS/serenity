#!/usr/bin/env -S bash ../.port_include.sh
port='ninja'
version='1.11.1'
files=(
    "https://github.com/ninja-build/ninja/archive/v${version}.tar.gz#31747ae633213f1eda3842686f83c2aa1412e0f5691d1c14dbbcc67fe7400cea"
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
