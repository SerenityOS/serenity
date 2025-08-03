#!/usr/bin/env -S bash ../.port_include.sh
port='pcre2'
version='10.45'
files=(
    "https://github.com/PCRE2Project/pcre2/releases/download/pcre2-${version}/pcre2-${version}.tar.gz#0e138387df7835d7403b8351e2226c1377da804e0737db0e071b48f07c9d12ee"
)
useconfigure='true'
