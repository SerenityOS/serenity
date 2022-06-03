#!/usr/bin/env -S bash ../.port_include.sh
port='sqlite'
version='3380500'
files="https://www.sqlite.org/2022/sqlite-autoconf-${version}.tar.gz sqlite-autoconf-${version}.tar.gz 5af07de982ba658fd91a03170c945f99c971f6955bc79df3266544373e39869c"
auth_type='sha256'
useconfigure='true'
use_fresh_config_sub='true'
workdir="sqlite-autoconf-${version}"
