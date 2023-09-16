#!/usr/bin/env -S bash ../.port_include.sh
port='qt6-qtsvg'
version='6.4.0'
workdir="qtsvg-everywhere-src-${version}"
useconfigure='true'
files=(
    "https://download.qt.io/official_releases/qt/$(cut -d. -f1,2 <<< ${version})/${version}/submodules/qtsvg-everywhere-src-${version}.tar.xz#03fdae9437d074dcfa387dc1f2c6e7e14fea0f989bf7e1aa265fd35ffc2c5b25"
)
configopts=(
    '-GNinja'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_CROSSCOMPILING=ON'
    '-DQT_FORCE_BUILD_TOOLS=ON'
)
depends=(
    'qt6-qtbase'
)

configure() {
    QT_HOST_PATH="$(qmake6 -query QT_HOST_PREFIX)"
    QT_HOST_CMAKE_PATH="$(qmake6 -query QT_HOST_LIBS)/cmake"
    QT_HOST_TOOLS='HostInfo CoreTools GuiTools WidgetsTools'
    QT_HOST_TOOLS_PATH="${QT_HOST_CMAKE_PATH}/Qt6%s/\n"

    for host_tool in ${QT_HOST_TOOLS}; do
        if [[ ! -d $(printf $QT_HOST_TOOLS_PATH $host_tool) ]]; then
            echo "You need to have Qt $version installed on the host (path "$(printf $QT_HOST_TOOLS_PATH $host_tool)" is missing)"
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
