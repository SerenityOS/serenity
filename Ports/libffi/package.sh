#!/usr/bin/env -S bash ../.port_include.sh
port=libffi
version=3.4.2
useconfigure=true
use_fresh_config_sub=true
files=(
    "https://github.com/libffi/libffi/releases/download/v${version}/libffi-${version}.tar.gz#540fb721619a6aba3bdeef7d940d8e9e0e6d2c193595bc243241b77ff9e93620"
)
