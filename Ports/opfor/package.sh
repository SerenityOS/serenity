#!/usr/bin/env -S bash ../.port_include.sh
port='opfor'
version='2022.07.14'  # Bogus version, this was the last time the commit hashes were updated.
_hlsdk_commit='04eba8e9edb92e500090bcc2b34274a5647283a5'
useconfigure='true'
depends=("xash3d-fwgs")
workdir="hlsdk-portable-${_hlsdk_commit}"
files="https://github.com/FWGS/hlsdk-portable/archive/${_hlsdk_commit}.tar.gz hlsdk-portable-${_hlsdk_commit}.tar.gz f1f70ffdce218dba9df0e3676f09ed4714b62c06bb73241a09ce8b3c3158ed01"
auth_type='sha256'
launcher_name='Half-Life: Opposing Force'
launcher_category='Games'
launcher_command='sh /home/anon/Games/halflife/opfor.sh'

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

    echo "Please remember to copy the 'valve/' and 'gearbox/' folders from your own Half-Life installation"
    echo "into ${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife/"
}
