#!/usr/bin/env -S bash ../.port_include.sh
port=qt6-qtbase
version=6.1.1
#depends="zlib"
workdir=qtbase-everywhere-src-6.1.1
useconfigure=true
files="https://download.qt.io/official_releases/qt/6.1/6.1.1/submodules/qtbase-everywhere-src-6.1.1.tar.xz qt6-qtbase-6.1.1.tar.xz"
configopts="-GNinja -DCMAKE_CROSSCOMPILING=ON -DQT_BUILD_TOOLS_WHEN_CROSSCOMPILING=ON -DCMAKE_TOOLCHAIN_FILE=${SERENITY_SOURCE_DIR}/Toolchain/CMake/CMakeToolchain.txt -DQT_HOST_PATH=/usr -DQT_FEATURE_cxx20=ON -DBUILD_SHARED_LIBS=OFF"

QT_HOST_PATH=/usr
QT_HOST_CMAKE_PATH=${QT_HOST_PATH}/lib64/cmake
QT_HOST_TOOLS="HostInfo CoreTools GuiTools WidgetsTools"
QT_HOST_TOOLS_PATH="${QT_HOST_CMAKE_PATH}/Qt6%s/\n"

QT_DISABLED_FEATURES="sql opengl dbus systemsemaphore sharedmemory thread network"

configure() {
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

    echo "================================================================================"
    echo "|                                 NOTICE                                       |"
    echo "================================================================================"
    echo -e "You have just installed a static testing build of Qt $version.\n"
    echo "Please be aware that this build is STATIC, so you'll have to rebuild everything dependening on it to benefit from new stuff. Plugins very likely won't work at all, unless built in directly into this package."
    echo "Also, the following Qt modules are disabled for now:"
    echo -e "\t" "$QT_DISABLED_FEATURES"
    echo "Work on enabling Qt modules will be hapenning here:"
    echo -e "\t" "https://github.com/SerenityOS/serenity/tree/master/Ports/qt6-qtbase"
    echo "The development of the Qt Serenity platform plugin is hapenning here. Fixes for things like input handling, window management and theme integration should go here:"
    echo -e "\t" "https://github.com/MartinBriza/QSerenityPlatform"
}

clean() {
    run ninja clean
}
