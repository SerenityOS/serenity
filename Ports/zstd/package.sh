#!/usr/bin/env -S bash ../.port_include.sh
port=zstd
version=1.5.2
files=(
    "https://github.com/facebook/zstd/releases/download/v${version}/zstd-${version}.tar.gz zstd-${version}.tar.gz 7c42d56fac126929a6a85dbc73ff1db2411d04f104fae9bdea51305663a83fd0"
)
useconfigure='true'
configopts=(
    "-Sbuild/cmake"
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
