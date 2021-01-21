#!/usr/bin/env -S bash ../.port_include.sh
port=stress-ng
version=0.11.23
workdir=stress-ng-${version}
files="https://github.com/ColinIanKing/stress-ng/archive/V${version}.tar.gz stress-ng-${version}.tar.gz"
makeopts="STATIC=1"
