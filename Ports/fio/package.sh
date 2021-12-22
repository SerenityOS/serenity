#!/usr/bin/env -S bash ../.port_include.sh
port=fio
version=3.29
files="https://brick.kernel.dk/snaps/${port}-${version}.tar.gz ${port}-${version}.tar.gz bea42d6f9d6c009f951135591e99787ff5fa9bc1425596d3d3b19339afc7bb0e"
auth_type=sha256
depends=("zlib")

export LDFLAGS=-ldl
