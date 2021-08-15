#!/usr/bin/env -S bash ../.port_include.sh
port=qt6-qtbase
version=6.1.1
#depends="zlib"
workdir=qtbase-everywhere-src-6.1.1
useconfigure=true
files="https://download.qt.io/official_releases/qt/6.1/6.1.1/submodules/qtbase-everywhere-src-6.1.1.tar.xz qt6-qtbase-6.1.1.tar.xz"
configopts="-GNinja -DCMAKE_CROSSCOMPILING=ON -DQT_BUILD_TOOLS_WHEN_CROSSCOMPILING=ON -DCMAKE_TOOLCHAIN_FILE=${SERENITY_SOURCE_DIR}/Toolchain/CMake/CMakeToolchain.txt -DQT_HOST_PATH=/usr -DQT_FEATURE_cxx20=ON -DBUILD_SHARED_LIBS=OFF"

configure() {
    QT_HOST_PATH=/usr
    QT_HOST_CMAKE_PATH=${QT_HOST_PATH}/lib64/cmake
    QT_HOST_TOOLS="HostInfo CoreTools GuiTools WidgetsTools"
    QT_HOST_TOOLS_PATH="${QT_HOST_CMAKE_PATH}/Qt6%s/\n"

    QT_DISABLED_FEATURES="sql opengl dbus systemsemaphore sharedmemory thread network"

    for i in ${QT_HOST_TOOLS}; do
        if [[ ! -d $(printf QT_HOST_TOOLS_PATH $i) ]]; then
            echo "You need to have local Qt $version installed (path "$(printf QT_HOST_TOOLS_PATH $i)" is missing"
        fi
    done

    MERGED_HOST_TOOLS=$(for i in ${QT_HOST_TOOLS}; do echo "-DQt6${i}_DIR=${QT_HOST_CMAKE_PATH}/Qt6${i}/"; done)
    MERGED_DISABLED_FEATURES=$(for i in ${QT_DISABLED_FEATURES}; do echo "-DQT_FEATURE_${i}=OFF"; done)

    run cmake $configopts ${MERGED_HOST_TOOLS} ${MERGED_DISABLED_FEATURES}
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
