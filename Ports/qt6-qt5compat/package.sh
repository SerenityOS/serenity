#!/usr/bin/env -S bash ../.port_include.sh
port='qt6-qt5compat'
version='6.4.0'
workdir="qt5compat-everywhere-src-${version}"
useconfigure='true'
files=(
    "https://download.qt.io/archive/qt/$(cut -d. -f1,2 <<< ${version})/${version}/submodules/qt5compat-everywhere-src-${version}.tar.xz#73475d0837f78246d509199f211a35c71fc36cccf64b3de258ebc6387194a4c0"
)
depends=(
    'qt6-qtbase'
    'libiconv'
)

configure() {
    export LDFLAGS='-liconv'
    run cmake -GNinja \
        -DCMAKE_TOOLCHAIN_FILE="${SERENITY_BUILD_DIR}/CMakeToolchain.txt" \
        -DCMAKE_CROSSCOMPILING=ON \
        -DQT_BUILD_TOOLS_WHEN_CROSSCOMPILING=ON \
        -DQT_HOST_PATH="$(qmake6 -query QT_HOST_PREFIX)" \
        -DQT_HOST_PATH_CMAKE_DIR="$(qmake6 -query QT_HOST_LIBS)/cmake" \
        -DQT_FEATURE_cxx20=ON
    unset LDFLAGS
}

build() {
    run ninja
}

install() {
    run ninja install
}
