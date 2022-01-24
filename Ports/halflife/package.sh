#!/usr/bin/env -S bash ../.port_include.sh
port="halflife"
version="1.0.0"
useconfigure="true"
depends=("SDL2" "fontconfig" "freetype")
workdir="."
files="https://github.com/SerenityPorts/xash3d-fwgs/archive/master.tar.gz xash3d_engine.tar.gz
https://github.com/SerenityPorts/hlsdk-xash3d/archive/master.tar.gz xash3d_hldll.tar.gz
https://github.com/FWGS/vgui-dev/archive/master.tar.gz vgui-dev.tar.gz
https://github.com/FWGS/mainui_cpp/archive/master.tar.gz mainui.tar.gz
https://github.com/FWGS/miniutl/archive/master.tar.gz miniutl.tar.gz"
launcher_name="Half-Life"
launcher_category="Games"
launcher_command="sh /home/anon/Games/halflife/hl.sh"

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"

# This one is a bit tricky to build, so I'm going a little bit off the script....
configure() {
    # Initialize submodules from tarballs
    [ -e ./xash3d-fwgs-master/mainui ] && rm -r ./xash3d-fwgs-master/mainui
    cp -r mainui_cpp-master/ ./xash3d-fwgs-master/mainui
    rmdir ./xash3d-fwgs-master/mainui/miniutl
    cp -r MiniUTL-master/ ./xash3d-fwgs-master/mainui/miniutl

    # Configure the shared object projects (client and game)
    cd ./hlsdk-xash3d-master
    ./waf configure -T release
    cd ../

    # Configure the engine itself...
    cd ./xash3d-fwgs-master
    ./waf configure --sdl2="${SERENITY_INSTALL_ROOT}/usr/local" --vgui=../vgui-dev-master/ -T release
    cd ../
}

build() {
    # Build the game and client
    cd ./hlsdk-xash3d-master
    ./waf build
    cd ../

    # Build the engine
    cd ./xash3d-fwgs-master
    ./waf build
    cd ../
}

install() {
    cd ./hlsdk-xash3d-master
    ./waf install --destdir=${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife
    cd ../

    cd ./xash3d-fwgs-master
    ./waf install --destdir=${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife/
    cd ../
}

post_install() {
    # On arch-linux systems, rename is installed as perl-rename
    if command -v perl-rename &> /dev/null
    then
        rename_command=perl-rename
    else
        rename_command=rename
    fi
    # Strip the output libraries of their "lib" prefix
    pushd ${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife/valve/cl_dlls/
    "$rename_command" 's/^...//' lib*
    popd
    pushd ${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife/valve/dlls/
    "$rename_command" 's/^...//' lib*
    popd

    # Create a launch script
    cat <<- 'EOF' > ${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife/hl.sh
#!/bin/sh
export LD_LIBRARY_PATH=/home/anon/Games/halflife/
scriptdir=$(dirname "$0")
cd $scriptdir
./xash3d -console
EOF
    chmod +x ${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife/hl.sh

    echo "Please remember to copy the 'valve/' folder from your own Half-Life installation"
    echo "into ${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife/"
}
