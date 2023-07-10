#!/usr/bin/env -S bash ../.port_include.sh
port='sqlite'
version='3410200'
files=(
    "https://www.sqlite.org/2023/sqlite-autoconf-${version}.tar.gz sqlite-autoconf-${version}.tar.gz e98c100dd1da4e30fa460761dab7c0b91a50b785e167f8c57acc46514fae9499"
)
useconfigure='true'
use_fresh_config_sub='true'
workdir="sqlite-autoconf-${version}"
