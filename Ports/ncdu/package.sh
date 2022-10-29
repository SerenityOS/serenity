#!/usr/bin/env -S bash ../.port_include.sh
port='ncdu'
version='2.2.1'
files="https://dev.yorhel.nl/download/ncdu-${version}.tar.gz ncdu-${version}.tar.gz 5e4af8f6bcd8cf7ad8fd3d7900dab1320745a0453101e9e374f9a77f72aed141"
auth_type='sha256'
useconfigure='true'
depends=("ncurses")
