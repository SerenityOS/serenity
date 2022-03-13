#!/usr/bin/env -S bash ../.port_include.sh

port=bdwgc
version=8.0.6
use_fresh_config_sub=true
files="https://github.com/ivmai/bdwgc/releases/download/v$version/gc-$version.tar.gz bdwgc.tar.gz 3b4914abc9fa76593596773e4da671d7ed4d5390e3d46fbf2e5f155e121bea11"
depends=(libatomic_ops)
workdir=gc-$version
auth_type=sha256

build() {
    cd $workdir
    mkdir build || true
    cd build
    cmake .. \
        -Denable_threads=ON \
        -DCMAKE_RUNTIME_OUTPUT_DIRECTORY="$DESTDIR/usr/local/bin" \
        -DCMAKE_LIBRARY_OUTPUT_DIRECTORY="$DESTDIR/usr/local/lib"
    cmake --build .
}

install() {
    cd ..
    cp -r include "$DESTDIR/usr/local/include/gc"
}
