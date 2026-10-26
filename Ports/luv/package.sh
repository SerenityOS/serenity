#!/usr/bin/env -S bash ../.port_include.sh
port='luv'
version='1.53.0-0'
files=(
    "https://github.com/luvit/luv/releases/download/${version}/luv-${version}.tar.gz#fccabc7bb37fcc3b84ab258a19a862c55f67abc2059af0a744a1ae64119dfc6c"
)
useconfigure='true'

depends=(
    'libuv'
    'luajit'
)

configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
    '-DWITH_SHARED_LIBUV=ON'
    '-DLUA_BUILD_TYPE=System'
    '-DBUILD_MODULE=OFF'
    '-DBUILD_SHARED_LIBS=ON'
)

configure() {
    run cmake -G Ninja -B build -S . "${configopts[@]}"
}

build() {
    run cmake --build build --parallel "${MAKEJOBS}"
}

install() {
    export DESTDIR="${SERENITY_INSTALL_ROOT}"
    run cmake --install build
}
