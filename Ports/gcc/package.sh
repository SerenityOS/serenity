#!/bin/sh ../.port_include.sh
port=gcc
version=8.3.0
useconfigure=true
configopts="--target=i686-pc-serenity --with-sysroot=/ --with-build-sysroot=$SERENITY_ROOT/Root --with-newlib --enable-languages=c,c++ --disable-lto --disable-nls"
files="https://ftp.gnu.org/gnu/gcc/gcc-8.3.0/gcc-8.3.0.tar.xz gcc-8.3.0.tar.xz"
makeopts="all-gcc all-target-libgcc all-target-libstdc++-v3"
installopts="DESTDIR=$SERENITY_ROOT/Root install-gcc install-target-libgcc install-target-libstdc++-v3"
depends="binutils"

fetch() {
    read url filename <<< $(echo "$files")    
    run_nocd curl -O "$url" -o "$filename"
    run_nocd tar xf "$filename"
    run contrib/download_prerequisites
    for f in patches/*; do
        run patch -p1 < "$f"
    done
}
build() {
    run make "$makeopts"
    run find ./host-i686-pc-serenity/gcc/ -maxdepth 1 -type f -executable -exec strip --strip-debug {} \; || echo
}

install() {
    run make "$installopts"
}
