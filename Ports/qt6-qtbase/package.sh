#!/usr/bin/env -S bash ../.port_include.sh
port=qt6-qtbase
version=6.2.2
workdir=qtbase-everywhere-src-${version}
useconfigure=true
files="https://download.qt.io/official_releases/qt/$(cut -d. -f1,2 <<< ${version})/${version}/submodules/qtbase-everywhere-src-${version}.tar.xz qt6-qtbase-${version}.tar.xz 85ab9180180c2eaf84cd11ae4c6d5a6a69f2f8fd7260aaccfd91a3e7e7232c1a"
auth_type="sha256"
configopts=(
    "-GNinja"
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_CROSSCOMPILING=ON"
    "-DQT_BUILD_TOOLS_WHEN_CROSSCOMPILING=ON"
    "-DQT_HOST_PATH=/usr"
    "-DQT_FEATURE_cxx20=ON"
    "-DINPUT_opengl=no"
)

QT_HOST_PATH=/usr
QT_HOST_CMAKE_PATH=${QT_HOST_PATH}/lib64/cmake
QT_HOST_TOOLS="HostInfo CoreTools GuiTools WidgetsTools"
QT_HOST_TOOLS_PATH="${QT_HOST_CMAKE_PATH}/Qt6%s/\n"

QT_DISABLED_FEATURES="sql opengl dbus systemsemaphore sharedmemory thread network"

configure() {
    for host_tool in ${QT_HOST_TOOLS}; do
        if [[ ! -d $(printf $QT_HOST_TOOLS_PATH $host_tool) ]]; then
            echo "You need to have Qt $version installed on the host (path "$(printf $QT_HOST_TOOLS_PATH $host_tool)" is missing)"
            exit 1
        fi
    done

    MERGED_HOST_TOOLS=$(for host_tool in ${QT_HOST_TOOLS}; do echo "-DQt6${host_tool}_DIR=${QT_HOST_CMAKE_PATH}/Qt6${host_tool}/"; done)
    MERGED_DISABLED_FEATURES=$(for feature in ${QT_DISABLED_FEATURES}; do echo "-DQT_FEATURE_${feature}=OFF"; done)

    run cmake ${configopts[@]} ${MERGED_HOST_TOOLS} ${MERGED_DISABLED_FEATURES}
}

build() {
    run ninja
}

install() {
    run ninja install

    echo "================================================================================"
    echo "|                                 NOTICE                                       |"
    echo "================================================================================"
    echo -e "You have just installed a testing build of Qt $version.\n"
    echo -e "GUI applications won't work without QSerenityPlatform!"
    echo -e "\t" "It's packaged as qt6-serenity"
    echo "The following Qt modules are disabled for now:"
    echo -e "\t" "$QT_DISABLED_FEATURES"
    echo "Work on enabling Qt modules will be happening here:"
    echo -e "\t" "https://github.com/SerenityOS/serenity/tree/master/Ports/qt6-qtbase"
    echo "The development of the Qt Serenity platform plugin is happening here. Fixes for things like input handling, window management and theme integration should go here:"
    echo -e "\t" "https://github.com/SerenityPorts/QSerenityPlatform"
}

clean() {
    run ninja clean
}
