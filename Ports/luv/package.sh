#!/usr/bin/env -S bash ../.port_include.sh
port='luv'
version='1.53.0-0'
files=(
    "https://github.com/luvit/luv/releases/download/${version}/luv-${version}.tar.gz#ce6260efb974647e7687f54a54a6810bc5eb81cdc14b45b69868a5ea048f96e1"
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
