#!/usr/bin/env -S bash ../.port_include.sh
port="opfor"
version="2022.05.01"  # Bogus version, this was the last time the commit hashes were updated.
_hlsdk_commit=2ffa0261e30a4b90082965bb2539b767c5686a5f
useconfigure="true"
depends=("SDL2" "halflife")
workdir="hlsdk-xash3d-${_hlsdk_commit}"
files="https://github.com/FWGS/hlsdk-xash3d/archive/${_hlsdk_commit}.tar.gz hlsdk-xash3d-${_hlsdk_commit}.tar.gz 3537f0ba3baead72beecfdc011cc17cce2fd2227d9c9797767c32a869c764f66"
auth_type="sha256"
launcher_name="Half-Life: Opposing Force"
launcher_category="Games"

# This one is a bit tricky to build, so I'm going a little bit off the script....
configure() {
    # Configure the shared object projects (client and game)
    run ./waf configure -T release
}

build() {
    # Build game shared object
    run ./waf build
}

install() {
    run ./waf install --destdir=${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife
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
    pushd ${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife/gearbox/cl_dlls/
    "$rename_command" 's/^...//' lib*
    popd
    pushd ${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife/gearbox/dlls/
    "$rename_command" 's/^...//' lib*
    popd

    # Create a launch script
    cat <<- 'EOF' > ${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife/opfor.sh
#!/bin/sh
export LD_LIBRARY_PATH=/home/anon/Games/halflife/
scriptdir=$(dirname "$0")
cd $scriptdir
./xash3d -console -game gearbox
EOF
    chmod +x ${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife/opfor.sh

    echo "Please remember to copy the 'gearbox/' folder from your own Half-Life installation"
    echo "into ${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife/"
}
