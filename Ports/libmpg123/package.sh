#!/usr/bin/env -S bash ../.port_include.sh
port=libmpg123
version=1.29.3
useconfigure=true
workdir=mpg123-${version}
use_fresh_config_sub=true
config_sub_paths=("build/config.sub")
files="https://download.sourceforge.net/project/mpg123/mpg123/${version}/mpg123-${version}.tar.bz2 mpg123-${version}.tar.bz2
https://download.sourceforge.net/project/mpg123/mpg123/${version}/mpg123-${version}.tar.bz2.sig mpg123-${version}.tar.bz2.sig"

auth_type="sig"
auth_import_key="D021FF8ECF4BE09719D61A27231C4CBC60D5CAFE"
auth_opts=("mpg123-${version}.tar.bz2.sig")
