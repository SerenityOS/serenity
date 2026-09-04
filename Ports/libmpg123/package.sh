#!/usr/bin/env -S bash ../.port_include.sh
port='libmpg123'
version='1.33.7'
useconfigure='true'
workdir="mpg123-${version}"
files=(
    "https://download.sourceforge.net/project/mpg123/mpg123/${version}/mpg123-${version}.tar.bz2#31d0e35a4ca567ec9b5ebda6c3062bb4435d6d3eacd6ef0d95cadd7854dc03ee"
)
