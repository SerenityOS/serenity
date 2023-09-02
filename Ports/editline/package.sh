#!/usr/bin/env -S bash ../.port_include.sh

port='editline'
version='1.17.1'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('aux/config.sub')
files=(
    "https://github.com/troglobit/editline/releases/download/${version}/editline-${version}.tar.gz#781e03b6a935df75d99fb963551e2e9f09a714a8c49fc53280c716c90bf44d26"
)
