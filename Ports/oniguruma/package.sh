#!/usr/bin/env -S bash ../.port_include.sh
port='oniguruma'
version='6.9.9'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "https://github.com/kkos/oniguruma/releases/download/v${version}/onig-${version}.tar.gz#60162bd3b9fc6f4886d4c7a07925ffd374167732f55dce8c491bfd9cd818a6cf"
)
workdir="onig-${version}"
