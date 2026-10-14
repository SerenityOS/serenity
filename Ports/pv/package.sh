#!/usr/bin/env -S bash ../.port_include.sh
port='pv'
version='1.12.0'
useconfigure='true'
configopts=(
    '--disable-nls'
)
files=(
    "https://ivarch.com/s/pv-${version}.tar.gz#31fdbdb449c7143cd2968567bef7599e9f031950e6158ee7bb76e40aebf6ffb8"
)
