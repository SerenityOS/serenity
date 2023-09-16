#!/usr/bin/env -S bash ../.port_include.sh
port=mixxx
useconfigure=true
version=2.3.6
depends=("chromaprint" "libfftw3f" "rubberband" "lame" "flac" "libopus" "libvorbis" "openssl" "qt6-qt5compat" "qt6-qtbase" "qt6-qtsvg" "qt6-serenity" "protobuf" "zlib")
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" 
    "-DSDL2_INCLUDE_DIR=${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"
    "-DFFTW_INCLUDE_DIR=${SERENITY_INSTALL_ROOT}/usr/local/include/FFTW"
    "-DFFTW_LIBRARY=${SERENITY_INSTALL_ROOT}/usr/local/lib/libfftw3f.a"
    "-Dmp3lame_INCLUDE_DIR=${SERENITY_INSTALL_ROOT}/usr/local/include/lame"
    "-DOPENGL_opengl_LIBRARY=${SERENITY_INSTALL_ROOT}/usr/lib/libgl.so"
    "-DOPENGL_INCLUDE_DIR=${SERENITY_INSTALL_ROOT}/usr/include/LibGL"
    "-DCMAKE_BUILD_TYPE=Release"
    "-DQT6=ON"
    "-DKEYFINDER=OFF"
    "-DOpenGL_GL_PREFERENCE=LEGACY"
    "-DQOPENGL=OFF"
    "-DSTATIC_DEPS=ON"
)
files=(
    "https://github.com/mixxxdj/mixxx/archive/refs/tags/${version}.tar.gz#0030d07c1506ccc13daa63d851921381b5bf838e9407cd666557d951ac093c52"
)
launcher_name="Mixxx"
launcher_category=Media
launcher_command="/bin/mixxx"
configure() {
    run cmake -G Ninja -B build -S . "${configopts[@]}"
}

build() {
    run cmake --build build
}

install() {
    run ninja install
}
