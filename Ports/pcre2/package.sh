#!/usr/bin/env -S bash ../.port_include.sh
port='pcre2'
version='10.40'
files=(
    "https://github.com/PhilipHazel/pcre2/releases/download/pcre2-${version}/pcre2-${version}.tar.gz pcre2-${version}.tar.gz ded42661cab30ada2e72ebff9e725e745b4b16ce831993635136f2ef86177724"
)
useconfigure='true'
