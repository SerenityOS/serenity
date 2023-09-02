#!/usr/bin/env -S bash ../.port_include.sh
port='chibi-scheme'
version='0.10'
useconfigure='false'
files=(
    "https://github.com/ashinn/chibi-scheme/archive/refs/tags/${version}.tar.gz ae1d2057138b7f438f01bfb1e072799105faeea1de0ab3cc10860adf373993b3"
)
localbuilddir="host_build"

export LD_LIBRARY_PATH=$localbuilddir/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}
export DYLD_LIBRARY_PATH=$localbuilddir/lib${DYLD_LIBRARY_PATH:+:$DYLD_LIBRARY_PATH}

build() {
    
    host_env

    run make PLATFORM=serenity
    run make "DESTDIR=$localbuilddir" PREFIX= PLATFORM=serenity install
    run make clean
    
    target_env

    run make PLATFORM=serenity "CHIBI=$localbuilddir/bin/chibi-scheme"
}

install() {
    run make \
        "DESTDIR=$DESTDIR" \
        PLATFORM=serenity \
        CHIBI=$localbuilddir/bin/chibi-scheme \
        CHIBI_MODULE_PATH="$localbuilddir/share/chibi:$localbuilddir/lib/chibi" \
        install
}
