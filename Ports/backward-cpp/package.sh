#!/usr/bin/env -S bash ../.port_include.sh
port='backward-cpp'
version='1.6'
files="https://github.com/bombela/backward-cpp/archive/refs/tags/v${version}.tar.gz backward-cpp-${version}.tar.gz c654d0923d43f1cea23d086729673498e4741fb2457e806cfaeaea7b20c97c10"
auth_type='sha256'
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
