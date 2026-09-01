#!/usr/bin/env -S bash ../.port_include.sh
port='pcre2'
version='10.48'
files=(
    "https://github.com/PCRE2Project/pcre2/releases/download/pcre2-${version}/pcre2-${version}.tar.gz#ebcc25aadf2a51fa1fefa9b8bc9e7a79b3dae86870a0f1152a22e42befd46888"
)
useconfigure='true'
