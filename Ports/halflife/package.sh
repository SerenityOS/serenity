#!/usr/bin/env -S bash ../.port_include.sh
port='halflife'
version='2022.12.26'  # Bogus version, this was the last time the commit hashes were updated.
_hlsdk_commit='63e3769c46ba7f502b53abdfdd55597e4130c0dd'
useconfigure='true'
depends=("xash3d-fwgs")
workdir="hlsdk-portable-${_hlsdk_commit}"
files=(
    "https://github.com/FWGS/hlsdk-portable/archive/${_hlsdk_commit}.tar.gz#b010c94ed400e44508f43706aeea68b82615c6ad22d2b1b892e8d6201d97503d"
)
launcher_name='Half-Life'
launcher_category='&Games'
launcher_command='sh /home/anon/Games/halflife/hl.sh'

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"

configure() {
    run ./waf configure -T release
}

build() {
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
