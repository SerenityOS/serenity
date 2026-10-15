#!/usr/bin/env -S bash ../.port_include.sh
port='xorriso'
version='1.5.8.pl02'
workdir='xorriso-1.5.8'
files=(
    "https://www.gnu.org/software/xorriso/xorriso-${version}.tar.gz#b1455ecafbf0692ddafe1d71002a96f2ce2d77f4deae602678261ce033f97bc8"
)
depends=(
    'libiconv'
)
useconfigure='true'
