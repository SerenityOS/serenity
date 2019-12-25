#!/bin/bash ../.port_include.sh
port=nesalizer
version=master
curlopts="-L"
makeopts="CONF=release"
files="https://github.com/SerenityOS/nesalizer/archive/master.zip nesalizer-master.zip"
depends=SDL2
