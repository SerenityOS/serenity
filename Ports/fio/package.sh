#!/usr/bin/env -S bash ../.port_include.sh
port='fio'
description='fio - Flexible I/O tester'
version='3.33'
website='https://fio.readthedocs.io/en/latest/'
files="https://brick.kernel.dk/snaps/${port}-${version}.tar.gz ${port}-${version}.tar.gz d2410e13e0f379d061d077cc5ae325835bb7c6186aa7bafc1df954cbc9b014fc"
auth_type='sha256'
depends=("zlib")

export LDFLAGS='-ldl'
