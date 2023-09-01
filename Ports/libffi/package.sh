#!/usr/bin/env -S bash ../.port_include.sh
port='libffi'
version='3.4.4'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "https://github.com/libffi/libffi/releases/download/v${version}/libffi-${version}.tar.gz#d66c56ad259a82cf2a9dfc408b32bf5da52371500b84745f7fb8b645712df676"
)
