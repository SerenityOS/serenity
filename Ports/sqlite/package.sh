#!/usr/bin/env -S bash ../.port_include.sh
port=sqlite
useconfigure="true"
use_fresh_config_sub="true"
version="3380200"
files="https://www.sqlite.org/2022/sqlite-autoconf-${version}.tar.gz sqlite-autoconf-${version}.tar.gz e7974aa1430bad690a5e9f79a6ee5c8492ada8269dc675875ad0fb747d7cada4"
auth_type=sha256
workdir="sqlite-autoconf-${version}"
