#!/usr/bin/env -S bash ../.port_include.sh
port=sqlite
useconfigure="true"
version="3350300"
files="https://www.sqlite.org/2021/sqlite-autoconf-${version}.tar.gz sqlite-autoconf-${version}.tar.gz"
workdir="sqlite-autoconf-${version}"
configopts="CFLAGS=-DHAVE_UTIME"
