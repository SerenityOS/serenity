#!/usr/bin/env -S bash ../.port_include.sh
port=sqlite
useconfigure="true"
version="3350300"
files="https://www.sqlite.org/2021/sqlite-autoconf-${version}.tar.gz sqlite-autoconf-${version}.tar.gz cadd4db9b528dfd0a7efde39f767cd83"
auth_type=md5
workdir="sqlite-autoconf-${version}"
