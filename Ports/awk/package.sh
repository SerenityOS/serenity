#!/usr/bin/env -S bash ../.port_include.sh
port='awk'
version='20261106'
useconfigure="false"
files=(
    "https://github.com/onetrueawk/awk/archive/refs/tags/${version}.tar.gz#1b354fe6fa0089a959ef912085e7a064236d15a7d3c9e6ce86456c3b87d3f464"
)

build() {
    run make "${makeopts[@]}"
    run mv a.out awk
}

install() {
    run mkdir -p ${SERENITY_INSTALL_ROOT}/usr/local/bin/
    run cp awk ${SERENITY_INSTALL_ROOT}/usr/local/bin/
}
