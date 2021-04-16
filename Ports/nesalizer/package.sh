#!/usr/bin/env -S bash ../.port_include.sh
port=nesalizer
version=master
makeopts="CONF=release EXTRA=-I${SERENITY_ROOT}/Build/i686/Root/usr/local/include/SDL2"
files="https://github.com/SerenityOS/nesalizer/archive/master.zip nesalizer-master.zip 0bbcde24c2366ced827ad63935f557cf"
auth_type=md5
depends=SDL2
