#!/usr/bin/env -S bash ../.port_include.sh
port='prboom-plus'
version='2.6.2'
useconfigure='true'
squashed_version=$(echo "${version}" | tr -d '.')
files="https://github.com/coelckers/prboom-plus/archive/refs/tags/v${version}.tar.gz prboom-plus-v4${version}.tar.gz 5cfeec96fbfe4fc3bd5dbc2b8d581ff5f6617dd74b2799680ba5b1e2e38c4aff
https://github.com/coelckers/prboom-plus/releases/download/v${version}/prboom-plus-${squashed_version}-w32.zip prboom-plus-w32.zip 20313e00d8841a618e23e7c671d65870194bee634468fecd2f3697ac05f21476"
workdir="prboom-plus-${version}/prboom2"
depends=("glu" "libmad" "libvorbis" "SDL2" "SDL2_image" "SDL2_mixer" "SDL2_net")
configopts=(
    "-DCMAKE_C_FLAGS=-I${SERENITY_INSTALL_ROOT}/usr/include/LibGL"
    "-DCMAKE_PREFIX_PATH=${SERENITY_INSTALL_ROOT}/usr/local"
    "-DFORCE_CROSSCOMPILE=ON"
    "-DOPENGL_gl_LIBRARY=${SERENITY_INSTALL_ROOT}/usr/lib/libgl.so"
    "-DWITH_PCRE=OFF"
    "-Wno-dev"
)

launcher_name='PrBoom+'
launcher_category='Games'
launcher_command='/usr/local/bin/prboom-plus -vidmode gl'
icon_file='ICONS/prboom-plus.ico'

configure() {
    run cmake -B build "${configopts[@]}"
}

build() {
    run make -C build "${makeopts[@]}"
}

install() {
    run make DESTDIR="${DESTDIR}" -C build install

    wad_directory="${SERENITY_INSTALL_ROOT}/usr/local/share/games/doom"
    mkdir -p "${wad_directory}"
    run cp "../../prboom-plus-${squashed_version}-w32/prboom-plus.wad" "${wad_directory}/"
}
