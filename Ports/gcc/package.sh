#!/bin/bash ../.port_include.sh
port=gcc
version=9.2.0
useconfigure=true
configopts="--target=i686-pc-serenity --with-sysroot=/ --with-build-sysroot=$SERENITY_ROOT/Root --with-newlib --enable-languages=c,c++ --disable-lto --disable-nls"
files="https://ftp.gnu.org/gnu/gcc/gcc-9.2.0/gcc-9.2.0.tar.xz gcc-9.2.0.tar.xz"
makeopts="all-gcc all-target-libgcc all-target-libstdc++-v3 -j $(nproc)"
installopts="DESTDIR=$SERENITY_ROOT/Root install-gcc install-target-libgcc install-target-libstdc++-v3"
depends="binutils"

fetch() {
    read url filename <<< $(echo "$files")
    if [ -f "$filename" ]; then
        echo "$filename already exists"
    else
        run_nocd curl -O "$url" -o "$filename"
    fi
    run_nocd tar xf "$filename"
    run contrib/download_prerequisites
    for f in patches/*; do
        run patch -p1 < "$f"
    done
}
build() {
    run make $makeopts
    run find ./host-i686-pc-serenity/gcc/ -maxdepth 1 -type f -executable -exec strip --strip-debug {} \; || echo
}

install() {
    run make $installopts
}
