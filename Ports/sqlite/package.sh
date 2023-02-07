#!/usr/bin/env -S bash ../.port_include.sh
port='sqlite'
version='3400100'
files="https://www.sqlite.org/2022/sqlite-autoconf-${version}.tar.gz sqlite-autoconf-${version}.tar.gz 2c5dea207fa508d765af1ef620b637dcb06572afa6f01f0815bd5bbf864b33d9"
auth_type='sha256'
useconfigure='true'
use_fresh_config_sub='true'
workdir="sqlite-autoconf-${version}"
