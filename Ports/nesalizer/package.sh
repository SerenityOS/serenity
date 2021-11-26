#!/usr/bin/env -S bash ../.port_include.sh
port=nesalizer
version=git
workdir=${port}-master
makeopts=("CONF=release" "EXTRA=-I${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2")
files="https://github.com/SerenityPorts/nesalizer/archive/master.zip nesalizer-master.zip"
depends=("SDL2")
