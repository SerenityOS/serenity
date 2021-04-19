#!/usr/bin/env -S bash ../.port_include.sh
port=nesalizer
version=master
makeopts="CONF=release EXTRA=-I${SERENITY_SOURCE_DIR}/Build/i686/Root/usr/local/include/SDL2"
files="https://github.com/SerenityOS/nesalizer/archive/master.zip nesalizer-master.zip"
depends=SDL2
