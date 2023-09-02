#!/usr/bin/env -S bash ../.port_include.sh
port='gemrb'
version='0.9.1'
useconfigure='true'
files=(
    "https://github.com/gemrb/gemrb/archive/refs/tags/v${version}.tar.gz#6e5dbcf7398d5566751f434b0d4647196bfbe9a813e3b65ad6a4ee2f1bbfb9ba"
)
depends=(
	'freetype'
	'libiconv'
	'python3'
	'SDL2'
    'SDL2_mixer'
	'zlib'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_INSTALL_PREFIX=${DESTDIR}/usr/local/"
    '-DCMAKE_BUILD_TYPE=Release'
    '-DDISABLE_WERROR=1'
    '-DSTATIC_LINK=ON'
    '-DSDL_BACKEND=SDL2'
)
icon_file='artwork/gemrb-logo.ico'
launcher_name='GemRB'
launcher_category='Games'
launcher_command='/usr/local/bin/gemrb'

configure() {
    mkdir -p "${PORT_BUILD_DIR}/gemrb-${version}-build"
    cd "${PORT_BUILD_DIR}/gemrb-${version}-build"
    cmake -G Ninja "${configopts[@]}" "${PORT_BUILD_DIR}/gemrb-${version}"
}

build() {
    ninja -C "${PORT_BUILD_DIR}/gemrb-${version}-build"
}

install() {
    ninja -C "${PORT_BUILD_DIR}/gemrb-${version}-build" install
}
