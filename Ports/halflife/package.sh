#!/usr/bin/env -S bash ../.port_include.sh
port="halflife"
version="2022.05.01"  # Bogus version, this was the last time the commit hashes were updated.
_fwgs_commit=5402e1a2597c40c603bd0f2b1a9cd6a16506ec84
_hlsdk_commit=808be9442f60b4388f68fcef8b2659d0cd6db17b
_vgui_commit=93573075afe885618ea15831e72d44bdacd65bfb
_mainui_commit=01e964fdc26f5dce1512c030d0dfd68e17be2858
_miniutl_commit=67c8c226c451f32ee3c98b94e04f8966092b70d3
useconfigure="true"
depends=("SDL2" "fontconfig" "freetype")
workdir="."
files="https://github.com/FWGS/xash3d-fwgs/archive/${_fwgs_commit}.tar.gz xash3d-fwgs-${_fwgs_commit}.tar.gz 1401f6c0cf619c48a8a40938b2acdffd327725ca0ab59804c518bddf821637f9
https://github.com/FWGS/hlsdk-xash3d/archive/${_hlsdk_commit}.tar.gz hlsdk-xash3d-${_hlsdk_commit}.tar.gz fd17436571341bd5e50739f22d84f9857f492637479144d01b1ffc1ead9d776b
https://github.com/FWGS/vgui-dev/archive/${_vgui_commit}.tar.gz vgui-dev-${_vgui_commit}.tar.gz eb9315fba8ae444fdae240c10afebaf7f3b157233bf1589f0af557b2286928fa
https://github.com/FWGS/mainui_cpp/archive/${_mainui_commit}.tar.gz mainui_cpp-${_mainui_commit}.tar.gz c8f6ce81596d5690044542074ac9bc69bbd43b5e5766f71363a8b5d4d382ad71
https://github.com/FWGS/MiniUTL/archive/${_miniutl_commit}.tar.gz MiniUTL-${_miniutl_commit}.tar.gz 7b7b26377854b3fc741c8d652d8b3c9c540512644943ca6efb63df941b2861e3"
auth_type=sha256
launcher_name="Half-Life"
launcher_category="Games"
launcher_command="sh /home/anon/Games/halflife/hl.sh"

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"

# This one is a bit tricky to build, so I'm going a little bit off the script....
configure() {
    # Initialize submodules from tarballs
    [ -e ./xash3d-fwgs-${_fwgs_commit}/mainui ] && rm -r ./xash3d-fwgs-${_fwgs_commit}/mainui
    cp -r mainui_cpp-${_mainui_commit}/ ./xash3d-fwgs-${_fwgs_commit}/mainui
    rmdir ./xash3d-fwgs-${_fwgs_commit}/mainui/miniutl
    cp -r MiniUTL-${_miniutl_commit}/ ./xash3d-fwgs-${_fwgs_commit}/mainui/miniutl

    # Configure the shared object projects (client and game)
    cd ./hlsdk-xash3d-${_hlsdk_commit}
    ./waf configure -T release
    cd ../

    # Configure the engine itself...
    cd ./xash3d-fwgs-${_fwgs_commit}
    ./waf configure --sdl2="${SERENITY_INSTALL_ROOT}/usr/local" --vgui=../vgui-dev-${_vgui_commit}/ -T release
    cd ../
}

build() {
    # Build the game and client
    cd ./hlsdk-xash3d-${_hlsdk_commit}
    ./waf build
    cd ../

    # Build the engine
    cd ./xash3d-fwgs-${_fwgs_commit}
    ./waf build
    cd ../
}

install() {
    cd ./hlsdk-xash3d-${_hlsdk_commit}
    ./waf install --destdir=${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife
    cd ../

    cd ./xash3d-fwgs-${_fwgs_commit}
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
