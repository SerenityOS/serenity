#!/usr/bin/env -S bash ../.port_include.sh
port='backward-cpp'
version='65a769f'
_commit='65a769ffe77cf9d759d801bc792ac56af8e911a3'
files=(
    "https://github.com/bombela/backward-cpp/archive/${_commit}.tar.gz#452d230984e55d92a761709a467a0cc13a7cd5e205a2f954269a7d9d79fc356f"
)
workdir="backward-cpp-${_commit}"
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
