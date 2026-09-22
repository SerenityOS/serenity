#!/usr/bin/env -S bash ../.port_include.sh
port='boost'
version='1.92.0'
useconfigure='true'
depends=(
    'zlib'
    'bzip2'
    'zstd'
    'xz'
    'libicu'
)
files=(
    "https://github.com/boostorg/boost/releases/download/boost-${version}/boost-${version}-cmake.tar.gz#f51707c27359a0df0cac1beada86de31bb5eed5e8285592dadec384df99c2984"
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DBOOST_ENABLE_MPI=OFF'
    '-DBOOST_ENABLE_PYTHON=OFF'
    # boost_stacktrace_from_exception uses dlsym(RTLD_NEXT, ...), which we don't have.
    '-DBOOST_STACKTRACE_ENABLE_FROM_EXCEPTION=OFF'
    '-DBUILD_TESTING=OFF'
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make "${installopts[@]}" install
}
