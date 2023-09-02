#!/usr/bin/env -S bash ../.port_include.sh
port=qt6-serenity
version=git
depends=("qt6-qtbase")
workdir=QSerenityPlatform-master
useconfigure=true
files=(
    "https://github.com/SerenityPorts/QSerenityPlatform/archive/master.zip#522cbb41e814a3d6553d3761380f9ce08e341fd5a8137ef8cfa9fe555317c6c0"
)
configopts=(
    "-GNinja"
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_CROSSCOMPILING=ON"
)

configure() {
    QT_HOST_PATH="$(qmake6 -query QT_HOST_PREFIX)"
    QT_HOST_CMAKE_PATH="$(qmake6 -query QT_HOST_LIBS)/cmake"
    QT_HOST_TOOLS="HostInfo CoreTools GuiTools WidgetsTools"
    QT_HOST_TOOLS_PATH="${QT_HOST_CMAKE_PATH}/Qt6%s/\n"

    for host_tool in ${QT_HOST_TOOLS}; do
        if [[ ! -d $(printf $QT_HOST_TOOLS_PATH $host_tool) ]]; then
            echo "You need to have Qt installed on the host (path "$(printf $QT_HOST_TOOLS_PATH $host_tool)" is missing"
            exit 1
        fi
    done

    MERGED_HOST_TOOLS=$(for host_tool in ${QT_HOST_TOOLS}; do echo "-DQt6${host_tool}_DIR=${QT_HOST_CMAKE_PATH}/Qt6${host_tool}/"; done)

    run cmake ${configopts[@]} "-DQT_HOST_PATH=${QT_HOST_PATH}" ${MERGED_HOST_TOOLS}
}

build() {
    run ninja
}

install() {
    run ninja install
}
