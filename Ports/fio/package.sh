#!/usr/bin/env -S bash ../.port_include.sh
port='fio'
version='3.30'
files="https://brick.kernel.dk/snaps/${port}-${version}.tar.gz ${port}-${version}.tar.gz 93998f838f72f871b36d60da132fcc3abaadd7b14c628ec95ee4092f2d277aed"
auth_type='sha256'
depends=("zlib")

export LDFLAGS='-ldl'
