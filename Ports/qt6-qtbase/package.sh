#!/usr/bin/env -S bash ../.port_include.sh
port='qt6-qtbase'
version='6.4.0'
workdir="qtbase-everywhere-src-${version}"
useconfigure='true'
files=(
    "https://download.qt.io/official_releases/qt/$(cut -d. -f1,2 <<< ${version})/${version}/submodules/qtbase-everywhere-src-${version}.tar.xz#cb6475a0bd8567c49f7ffbb072a05516ee6671171bed55db75b22b94ead9b37d"
)
configopts=(
    '-GNinja'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_CROSSCOMPILING=ON'
    '-DQT_FORCE_BUILD_TOOLS=ON'
    '-DQT_FEATURE_cxx20=ON'
    '-DQT_FEATURE_ssl=ON'
    '-DQT_FEATURE_zstd=ON'
    '-DQT_FEATURE_sql=ON'
    '-DINPUT_opengl=no'
)
depends=(
    'double-conversion'
    'md4c'
    'openssl'
    'sqlite'
    'zstd'
)

QT_DISABLED_FEATURES='opengl dbus systemsemaphore sharedmemory dnslookup'

configure() {

    QT_HOST_PATH="$(qmake6 -query QT_HOST_PREFIX)"
    QT_HOST_CMAKE_PATH="$(qmake6 -query QT_HOST_LIBS)/cmake"
    QT_HOST_TOOLS="HostInfo CoreTools GuiTools WidgetsTools"
    QT_HOST_TOOLS_PATH="${QT_HOST_CMAKE_PATH}/Qt6%s/\n"

    for host_tool in ${QT_HOST_TOOLS}; do
        if [[ ! -d $(printf $QT_HOST_TOOLS_PATH $host_tool) ]]; then
            echo "You need to have Qt $version installed on the host (path "$(printf $QT_HOST_TOOLS_PATH $host_tool)" is missing)"
            exit 1
        fi
    done

    MERGED_HOST_TOOLS=$(for host_tool in ${QT_HOST_TOOLS}; do echo "-DQt6${host_tool}_DIR=${QT_HOST_CMAKE_PATH}/Qt6${host_tool}/"; done)
    MERGED_DISABLED_FEATURES=$(for feature in ${QT_DISABLED_FEATURES}; do echo "-DQT_FEATURE_${feature}=OFF"; done)

    run cmake ${configopts[@]} "-DQT_HOST_PATH=${QT_HOST_PATH}" ${MERGED_HOST_TOOLS} ${MERGED_DISABLED_FEATURES}
}

build() {
    run ninja
}

install() {
    run ninja install

    echo '================================================================================'
    echo '|                                 NOTICE                                       |'
    echo '================================================================================'
    echo -e "You have just installed a testing build of Qt $version.\n"
    echo -e $'GUI applications won\'t work without QSerenityPlatform!'
    echo -e $'\tIt\'s packaged as qt6-serenity'
    echo 'The following Qt modules are disabled for now:'
    echo -e $"\t$QT_DISABLED_FEATURES"
    echo 'Work on enabling Qt modules will be happening here:'
    echo -e $'\thttps://github.com/SerenityOS/serenity/tree/master/Ports/qt6-qtbase'
    echo 'The development of the Qt Serenity platform plugin is happening here. Fixes for things like input handling, window management and theme integration should go here:'
    echo -e $'\thttps://github.com/SerenityPorts/QSerenityPlatform'
}
