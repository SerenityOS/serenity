#!/bin/sh ../.port_include.sh
port=quake
version=0.65
workdir=SerenityQuake-master
useconfigure=false
curlopts="-L"
files="https://github.com/SerenityOS/SerenityQuake/archive/master.tar.gz quake.tar.gz"
makeopts="V=1 SYMBOLS_ON=Y"
depends=SDL2
