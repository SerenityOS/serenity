#!/usr/bin/env -S bash ../.port_include.sh
port='oniguruma'
version='6.9.10'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "https://github.com/kkos/oniguruma/releases/download/v${version}/onig-${version}.tar.gz#2a5cfc5ae259e4e97f86b68dfffc152cdaffe94e2060b770cb827238d769fc05"
)
workdir="onig-${version}"
