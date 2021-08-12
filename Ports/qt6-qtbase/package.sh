#!/usr/bin/env -S bash ../.port_include.sh
port=Qt6
version=6.1.1
workdir=qtbase-everywhere-src-6.1.1
useconfigure=true
files="https://download.qt.io/official_releases/qt/6.1/6.1.1/submodules/qtbase-everywhere-src-6.1.1.tar.xz qt6-qtbase-6.1.1.tar.xz"
configopts="-GNinja -DCMAKE_CROSSCOMPILING=ON -DQT_BUILD_TOOLS_WHEN_CROSSCOMPILING=ON -DCMAKE_TOOLCHAIN_FILE=${SERENITY_SOURCE_DIR}/Toolchain/CMake/CMakeToolchain.txt -DQT_HOST_PATH=/usr/lib64/qt6 -DQt6HostInfo_DIR=/usr/lib64/cmake/Qt6HostInfo -DQt6CoreTools_DIR=//usr/lib64/cmake/Qt6CoreTools/ -DQt6GuiTools_DIR=/usr/lib64/cmake/Qt6GuiTools/ -DQt6WidgetsTools_DIR=/usr/lib64/cmake/Qt6WidgetsTools/  -DQT_FEATURE_sql=OFF -DQT_FEATURE_opengl=OFF -DQT_FEATURE_gui=OFF -DQT_FEATURE_network=OFF -DQT_FEATURE_concurrent=OFF  -DQT_FEATURE_testlib=OFF -DQT_FEATURE_systemsemaphore=OFF -DQT_FEATURE_sharedmemory=OFF -DQT_FEATURE_thread=OFF"

configure() {
    run cmake $configopts
}

build() {
    run ninja
}

install() {
    run ninja install
}

clean() {
    run ninja clean
}
