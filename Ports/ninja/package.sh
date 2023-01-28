#!/usr/bin/env -S bash ../.port_include.sh
port='ninja'
description='Ninja'
version='1.11.0'
website='https://ninja-build.org/'
files="https://github.com/ninja-build/ninja/archive/v${version}.tar.gz ninja-v${version}.tar.gz 3c6ba2e66400fe3f1ae83deb4b235faf3137ec20bd5b08c29bfc368db143e4c6"
auth_type='sha256'

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
