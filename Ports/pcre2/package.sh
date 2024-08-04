#!/usr/bin/env -S bash ../.port_include.sh
port='pcre2'
version='10.44'
files=(
    "https://github.com/PhilipHazel/pcre2/releases/download/pcre2-${version}/pcre2-${version}.tar.gz#86b9cb0aa3bcb7994faa88018292bc704cdbb708e785f7c74352ff6ea7d3175b"
)
useconfigure='true'
