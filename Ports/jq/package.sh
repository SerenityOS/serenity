#!/usr/bin/env -S bash ../.port_include.sh
port='jq'
version='1.7.1'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('config/config.sub')
files=(
    "https://github.com/jqlang/jq/releases/download/jq-${version}/jq-${version}.tar.gz#478c9ca129fd2e3443fe27314b455e211e0d8c60bc8ff7df703873deeee580c2"
)
depends=(
    'oniguruma'
)
