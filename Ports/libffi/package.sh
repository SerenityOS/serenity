#!/usr/bin/env -S bash ../.port_include.sh
port='libffi'
version='3.4.5'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "https://github.com/libffi/libffi/releases/download/v${version}/libffi-${version}.tar.gz#96fff4e589e3b239d888d9aa44b3ff30693c2ba1617f953925a70ddebcc102b2"
)
