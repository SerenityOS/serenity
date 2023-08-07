#!/usr/bin/env -S bash ../.port_include.sh
port=awk
version=20220122
useconfigure="false"
files=(
    "https://github.com/onetrueawk/awk/archive/refs/tags/${version}.tar.gz 720a06ff8dcc12686a5176e8a4c74b1295753df816e38468a6cf077562d54042"
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
