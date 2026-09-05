#!/usr/bin/env -S bash ../.port_include.sh
port=sed
version=4.9
useconfigure="true"
use_fresh_config_sub="true"
config_sub_paths=("build-aux/config.sub")
files=(
    "mirror://gnu/sed/sed-${version}.tar.gz#d1478a18f033a73ac16822901f6533d30b6be561bcbce46ffd7abce93602282e"
)
