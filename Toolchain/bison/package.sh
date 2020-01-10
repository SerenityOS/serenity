#!/bin/bash ../.toolchain_include.sh

package=bison
version=3.5
useconfigure=true
configopts="--target=$TARGET --with-sysroot=\"$SYSROOT\" --enable-shared --disable-nls"

filename="bison-${version}.tar.xz"
files="http://ftp.gnu.org/gnu/bison/$filename $filename c0230be066069f33c8445766833f3205"
