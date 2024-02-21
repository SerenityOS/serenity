#!/usr/bin/env -S bash ../.port_include.sh
port='poppler'
version='24.02.0'
version_data='0.4.12'
files=(
    "https://poppler.freedesktop.org/poppler-${version}.tar.xz#19187a3fdd05f33e7d604c4799c183de5ca0118640c88b370ddcf3136343222e"
    "https://poppler.freedesktop.org/poppler-data-${version_data}.tar.gz#c835b640a40ce357e1b83666aabd95edffa24ddddd49b8daff63adb851cdab74"
)
depends=(
    'boost'
    'curl'
    'fontconfig'
    'freetype'
    'gpgme'
    'lcms2'
    'libjpeg'
    'libpng'
    'libtiff'
    'openjpeg'
)
useconfigure='true'
configopts=(
    '-B build'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=release'
    '-DENABLE_CPP=OFF'
    '-DBUILD_CPP_TESTS=OFF'
    '-DENABLE_GLIB=OFF'
    '-DENABLE_GOBJECT_INTROSPECTION=OFF'
    '-DENABLE_GPGME=OFF' # Enabling GPGME causes the program to crash (#23557)
    '-DENABLE_NSS3=OFF'
    '-DENABLE_QT5=OFF'
    '-DBUILD_QT5_TESTS=OFF'
    '-DENABLE_QT6=OFF'
    '-DBUILD_QT6_TESTS=OFF'
    '-DBUILD_GTK_TESTS=OFF'
    '-DBUILD_MANUAL_TESTS=OFF'
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
