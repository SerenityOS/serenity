#!/usr/bin/env -S bash ../.port_include.sh
port=awk
version=20240728
useconfigure="false"
files=(
    "https://github.com/onetrueawk/awk/archive/refs/tags/${version}.tar.gz#2d479817f95d5997fc4348ecebb1d8a1b25c81cebedb46ca4f59434247e08543"
)
patchlevel=1

build() {
    run make "${makeopts[@]}"
    run mv a.out awk
}

install() {
    run mkdir -p ${SERENITY_INSTALL_ROOT}/usr/local/bin/
    run cp awk ${SERENITY_INSTALL_ROOT}/usr/local/bin/
}
