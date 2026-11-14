#!/usr/bin/env -S bash ../.port_include.sh
port='expat'
version='2.10.0'
useconfigure='true'
files=(
    "https://github.com/libexpat/libexpat/releases/download/R_${version//./_}/expat-${version}.tar.xz#c624ba2998fc65df1163a49641ff3f9ab73138b11641d92c86bee0fb01b1bc7f"
)
