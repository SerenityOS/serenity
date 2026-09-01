#!/usr/bin/env -S bash ../.port_include.sh
port='jq'
version='1.8.2'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('config/config.sub')
files=(
    "https://github.com/jqlang/jq/releases/download/jq-${version}/jq-${version}.tar.gz#71b8d6e8f5fe81f6c6d0d110e3892251f6ce76ed095abd315e26e6e1193af3af"
)
depends=(
    'oniguruma'
)
