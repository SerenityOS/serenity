#!/usr/bin/env -S bash ../.port_include.sh
port='libyaml'
version='0.2.5'
files=(
    "https://github.com/yaml/libyaml/releases/download/${version}/yaml-${version}.tar.gz#c642ae9b75fee120b2d96c712538bd2cf283228d2337df2cf2988e3c02678ef4"
)
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=("config/config.sub")
workdir="yaml-${version}"
