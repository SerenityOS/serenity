#!/bin/bash ../.port_include.sh
port=sqlite
version=3.33.0
workdir="sqlite-src-3330000"
useconfigure="true"
files="https://www.sqlite.org/2020/sqlite-src-3330000.zip sqlite-src-3330000.zip"
configopts="--target=i686-pc-serenity --with-sysroot=/ --with-build-sysroot=$SERENITY_ROOT/Build/Root
	--disable-tcl --disable-editline --disable-readline --disable-amalgamation LDFLAGS=-lpthread"
depends="zlib"
