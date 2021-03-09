#!/usr/bin/env -S bash ../.port_include.sh

port="openttd"
version="1.10.3"
depends="SDL2"
useconfigure=true
files="https://cdn.openttd.org/openttd-releases/${version}/${port}-${version}-source.tar.xz ${port}-${version}-source.tar.xz"

set +u
export CFLAGS_BUILD="$CFLAGS_BUILD -m32"
export CXXFLAGS_BUILD="$CXXFLAGS_BUILD -m32"
export CXXFLAGS="$CXXFLAGS"
set -u

configure() {
    run chmod +x ./configure

    # Needed because code gen keep itself in a bad state after failure.
    run make clean -i && true

    run ./configure --host=i686-pc-serenity --without-liblzo2 --cc-build="$CC_BUILD" --cxx-build="$CXX_BUILD" --with-threads
}
