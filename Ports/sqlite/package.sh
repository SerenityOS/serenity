#!/usr/bin/env -S bash ../.port_include.sh
port=sqlite
useconfigure="true"
version="3350300"
files="https://www.sqlite.org/2021/sqlite-autoconf-${version}.tar.gz sqlite-autoconf-${version}.tar.gz ecbccdd440bdf32c0e1bb3611d635239e3b5af268248d130d0445a32daf0274b"
auth_type=sha256
workdir="sqlite-autoconf-${version}"
