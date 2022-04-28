#!/usr/bin/env -S bash ../.port_include.sh
port=libexpat
version=2.4.8
useconfigure=true
use_fresh_config_sub=true
config_sub_path=conftools/config.sub
files="https://github.com/libexpat/libexpat/releases/download/R_${version//./_}/expat-${version}.tar.xz expat-${version}.tar.xz
https://github.com/libexpat/libexpat/releases/download/R_${version//./_}/expat-${version}.tar.xz.asc expat-${version}.tar.xz.asc"
workdir=expat-${version}
auth_type="sig"
auth_import_key="CB8DE70A90CFBF6C3BF5CC5696262ACFFBD3AEC6"
auth_opts=("expat-${version}.tar.xz.asc" "expat-${version}.tar.xz")
