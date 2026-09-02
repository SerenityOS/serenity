#!/usr/bin/env -S bash ../.port_include.sh
port='awk'
version='20260426'
useconfigure="false"
files=(
    "https://github.com/onetrueawk/awk/archive/refs/tags/${version}.tar.gz#7ae5b9fc6a8149bc45ea0ba3ba434a69a16d1460d19f6d01b6f04cc885b8e02b"
)

build() {
    run make "${makeopts[@]}"
    run mv a.out awk
}

install() {
    run mkdir -p ${SERENITY_INSTALL_ROOT}/usr/local/bin/
    run cp awk ${SERENITY_INSTALL_ROOT}/usr/local/bin/
}
