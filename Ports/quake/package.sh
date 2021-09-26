#!/usr/bin/env -S bash ../.port_include.sh
port=quake
version=0.65
workdir=SerenityQuake-master
useconfigure=false
files="https://github.com/SerenityPorts/SerenityQuake/archive/master.tar.gz quake.tar.gz"
makeopts=("V=1" "SYMBOLS_ON=Y")
depends=("SDL2")
launcher_name=Quake
launcher_category=Games
launcher_command=quake

# FIXME: Uhh, the things in this directory are not supposed to run on the host, why add this to $PATH?!
export PATH="${SERENITY_INSTALL_ROOT}/usr/bin:${PATH}"
