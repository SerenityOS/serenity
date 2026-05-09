#!/usr/bin/env -S bash ../.port_include.sh
port='luv'
version='1.52.1-0'
files=(
    "https://github.com/luvit/luv/releases/download/${version}/luv-${version}.tar.gz#3e6eb820a3aee034f85f9cce9bd77b5d42f34d128a1ccec877adf28c913577c7"
)
useconfigure='true'

depends=(
    'libuv'
    'luajit'
)

configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_INSTALL_PREFIX=${DESTDIR}/usr/local/"
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
    run cmake --install build
}
