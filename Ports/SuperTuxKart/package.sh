#!/usr/bin/env -S bash ../.port_include.sh
port=SuperTuxKart
useconfigure='true'
version='1.5'
archive_hash='33cf8841e4ff4082d80b9248014295bbbea61d14683e86dff100e3ab8f7b27cb'
files=(
    "https://github.com/supertuxkart/stk-code/releases/download/${version}/SuperTuxKart-${version}-src.tar.gz#$archive_hash"
)
workdir="${port}-${version}-src"
launcher_name='SuperTuxKart'
launcher_category='&Games'
launcher_command='/usr/local/bin/supertuxkart'
icon_file='data/supertuxkart_16.png'
depends=(
    'curl'
    'freetype'
    'glu'
    'harfbuzz'
    'libjpeg'
    'libopenal'
    'libogg'
    'libpng'
    'libvorbis'
    'openssl'
    'SDL2'
    'sqlite'
    'zlib'
)

configopts=(
    '-DCMAKE_BUILD_TYPE=STKRelease'
    "-DCMAKE_INSTALL_PREFIX=${SERENITY_INSTALL_ROOT}/usr/local"
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DBUILD_RECORDER=OFF'
    '-DBUILD_SHARED_LIBS=OFF'
    '-DUSE_WIIUSE=OFF'
    '-DUSE_DNS_C=ON'
    '-DNO_SHADERC=ON'
    '-Bbuild'
    '-GNinja'
    "-DCMAKE_C_FLAGS=-I${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2 -lcurl -lharfbuzz -lfreetype"
    "-DCMAKE_CXX_FLAGS=-I${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"
    '-DUSE_IPV6=OFF'
    '-DUSE_SYSTEM_ENET=OFF'
)

configure() {
    run cmake "${configopts[@]}" .
}

build() {
    run ninja -C build
}

install() {
    run ninja -C build install
}
