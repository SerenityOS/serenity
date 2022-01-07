#!/usr/bin/env -S bash ../.port_include.sh
port=qt6-serenity
version=git
depends=("qt6-qtbase")
workdir=QSerenityPlatform-master
useconfigure=true
files="https://github.com/SerenityPorts/QSerenityPlatform/archive/master.zip QSerenityPlatform-git.zip"
configopts=(
    "-GNinja"
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_CROSSCOMPILING=ON"
    "-DQT_HOST_PATH=/usr"
)

QT_HOST_PATH=/usr
QT_HOST_CMAKE_PATH=${QT_HOST_PATH}/lib64/cmake
QT_HOST_TOOLS="HostInfo CoreTools GuiTools WidgetsTools"
QT_HOST_TOOLS_PATH="${QT_HOST_CMAKE_PATH}/Qt6%s/\n"

configure() {
    for host_tool in ${QT_HOST_TOOLS}; do
        if [[ ! -d $(printf $QT_HOST_TOOLS_PATH $host_tool) ]]; then
            echo "You need to have Qt installed on the host (path "$(printf $QT_HOST_TOOLS_PATH $host_tool)" is missing"
            exit 1
        fi
    done

    MERGED_HOST_TOOLS=$(for host_tool in ${QT_HOST_TOOLS}; do echo "-DQt6${host_tool}_DIR=${QT_HOST_CMAKE_PATH}/Qt6${host_tool}/"; done)

    run cmake ${configopts[@]} ${MERGED_HOST_TOOLS}
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
