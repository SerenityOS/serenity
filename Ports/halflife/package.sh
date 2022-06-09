#!/usr/bin/env -S bash ../.port_include.sh
port="halflife"
version="2022.05.01"  # Bogus version, this was the last time the commit hashes were updated.
_hlsdk_commit=808be9442f60b4388f68fcef8b2659d0cd6db17b
useconfigure="true"
depends=("xash3d-fwgs")
workdir="hlsdk-xash3d-${_hlsdk_commit}"
files="https://github.com/FWGS/hlsdk-xash3d/archive/${_hlsdk_commit}.tar.gz hlsdk-xash3d-${_hlsdk_commit}.tar.gz fd17436571341bd5e50739f22d84f9857f492637479144d01b1ffc1ead9d776b"
auth_type=sha256
launcher_name="Half-Life"
launcher_category="Games"
launcher_command="sh /home/anon/Games/halflife/hl.sh"

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
