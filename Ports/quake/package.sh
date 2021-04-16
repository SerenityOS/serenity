#!/usr/bin/env -S bash ../.port_include.sh
port=quake
version=0.65
workdir=SerenityQuake-master
useconfigure=false
files="https://github.com/SerenityOS/SerenityQuake/archive/master.tar.gz quake.tar.gz 09400bae2dc667964ba6ce815962cb59"
auth_type=md5
makeopts="V=1 SYMBOLS_ON=Y "
depends=SDL2

# FIXME: Uhh, the things in this directory are not supposed to run on the host, why add this to $PATH?!
export PATH="${SERENITY_BUILD_DIR}/Root/usr/bin:${PATH}"
