#!/usr/bin/env -S bash ../.port_include.sh
port='sqlite'
version='3430000'
files=(
    "https://www.sqlite.org/2023/sqlite-autoconf-${version}.tar.gz#49008dbf3afc04d4edc8ecfc34e4ead196973034293c997adad2f63f01762ae1"
)
useconfigure='true'
use_fresh_config_sub='true'
workdir="sqlite-autoconf-${version}"
