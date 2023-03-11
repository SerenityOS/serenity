#!/usr/bin/env -S bash ../.port_include.sh
port=SuperTuxKart
useconfigure="false"
version="1.4"
archive_hash="9890392419baf4715313f14d5ad60746f276eed36eb580636caf44e2532c0f03"
files="https://github.com/supertuxkart/stk-code/releases/download/${version}/supertuxkart-${version}-src.tar.xz ${port}-${version}-src.tar.xz $archive_hash"
auth_type=sha256
depends=("curl" "freetype" "glu" "harfbuzz" "libjpeg" "libopenal" "libogg" "libpng" "libvorbis" "openssl" "SDL2" "sqlite" "zlib")
workdir="${port}-${version}-src"

configopts=(
    "-DCMAKE_BUILD_TYPE=STKRelease"
    "-DCMAKE_INSTALL_PREFIX=${SERENITY_INSTALL_ROOT}/opt/SuperTuxKart"
    "-DSTK_INSTALL_DATA_DIR=${SERENITY_INSTALL_ROOT}/opt/SuperTuxKart"
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DBUILD_RECORDER=OFF"
    "-DBUILD_SHARED_LIBS=OFF"
    "-DUSE_WIIUSE=OFF"
)

launcher_name=SuperTuxKart
launcher_category=Games
launcher_command=/usr/local/bin/supertuxkart

export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/include/LibGL -I${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"

configure() {
    run cmake "${configopts[@]}" .
}

install() {
    run make install
}
