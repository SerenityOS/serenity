#!/usr/bin/env -S bash ../.port_include.sh
port='pcre2'
version='10.49'
files=(
    "https://github.com/PCRE2Project/pcre2/releases/download/pcre2-${version}/pcre2-${version}.tar.gz#929f0b20e62879252a15886b06c89f1edef61a363cbd5826fb041080a5e557ae"
)
useconfigure='true'
