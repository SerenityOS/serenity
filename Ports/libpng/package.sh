#!/usr/bin/env -S bash ../.port_include.sh
port='libpng'
version='1.6.58'
useconfigure='true'
configopts=(
    '--disable-static'
    '--enable-shared'
)
files=(
    "https://download.sourceforge.net/libpng/libpng-${version}.tar.gz#8c9b05b675ca7301a458df2c2e46f26e1d41ff36b8863f8c33530bc58c2e6225"
)
depends=(
    'zlib'
)
