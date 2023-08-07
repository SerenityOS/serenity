#!/usr/bin/env -S bash ../.port_include.sh
port='poppler'
version='23.07.0'
version_data='0.4.12'
files=(
    "https://poppler.freedesktop.org/poppler-${version}.tar.xz f29b4b4bf47572611176454c8f21506d71d27eca5011a39aa44038b30b957db0"
    "https://poppler.freedesktop.org/poppler-data-${version_data}.tar.gz c835b640a40ce357e1b83666aabd95edffa24ddddd49b8daff63adb851cdab74"
)
depends=(
    'fontconfig'
    'freetype'
    'libjpeg'
    'libpng'
    'libtiff'
)
useconfigure='true'
configopts=(
    '-B build'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=release'
    '-DENABLE_BOOST=OFF'
    '-DENABLE_CPP=OFF'
    '-DBUILD_CPP_TESTS=OFF'
    '-DENABLE_QT5=OFF'
    '-DBUILD_QT5_TESTS=OFF'
    '-DENABLE_QT6=OFF'
    '-DBUILD_QT6_TESTS=OFF'
    '-DBUILD_GTK_TESTS=OFF'
    '-DBUILD_MANUAL_TESTS=OFF'
    '-DENABLE_GLIB=OFF'
    '-DENABLE_GOBJECT_INTROSPECTION=OFF'
    '-DENABLE_GTK_DOC=OFF'
    '-DENABLE_LIBOPENJPEG=unmaintained'
)

configure() {
    run cmake "${configopts[@]}"
}

build() {
    run make -C build "${makeopts[@]}"
}

install() {
    run make -C build install "${installopts[@]}"
    cd "poppler-data-${version_data}"
    run_nocd make install datadir=${SERENITY_INSTALL_ROOT}/usr/local
}
