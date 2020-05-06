#!/bin/bash ../.port_include.sh
port=quake
version=0.65
workdir=SerenityQuake-master
useconfigure=false
files="https://github.com/SerenityOS/SerenityQuake/archive/master.tar.gz quake.tar.gz"
makeopts="V=1 SYMBOLS_ON=Y "
depends=SDL2

export PATH=${SERENITY_ROOT}/Build/Root/usr/bin:$PATH
