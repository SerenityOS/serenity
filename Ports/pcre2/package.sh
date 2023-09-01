#!/usr/bin/env -S bash ../.port_include.sh
port='pcre2'
version='10.42'
files=(
    "https://github.com/PhilipHazel/pcre2/releases/download/pcre2-${version}/pcre2-${version}.tar.gz#c33b418e3b936ee3153de2c61cc638e7e4fe3156022a5c77d0711bcbb9d64f1f"
)
useconfigure='true'
