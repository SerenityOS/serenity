#!/usr/bin/env -S bash ../.port_include.sh
port='backward-cpp'
version='65a769f'
files=(
    "https://github.com/bombela/backward-cpp/tarball/65a769ffe77cf9d759d801bc792ac56af8e911a3 backward-cpp-${version}.tar.gz 233271162bf09ce7c41026416e5d6f59a66f42f83c3ea370f110980ac219144a"
)
workdir="bombela-backward-cpp-${version}"
useconfigure='true'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
depends=(
    'binutils'
    'zlib'
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
    run mkdir -p ${SERENITY_INSTALL_ROOT}/usr/local/bin/backtrace_tests
    run_nocd cp -r ${workdir}/test_* ${SERENITY_INSTALL_ROOT}/usr/local/bin/backtrace_tests
}
