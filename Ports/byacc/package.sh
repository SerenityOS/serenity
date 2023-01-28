#!/usr/bin/env -S bash ../.port_include.sh
port=byacc
description='Berkeley Yacc'
version=20220128
website='https://invisible-island.net/byacc/byacc.html'
files="https://invisible-mirror.net/archives/byacc/byacc-${version}.tgz byacc-${version}.tgz
https://invisible-mirror.net/archives/byacc/byacc-${version}.tgz.asc byacc-${version}.tgz.asc"
useconfigure=true
auth_type="sig"
auth_import_key="19882D92DDA4C400C22C0D56CC2AF4472167BE03"
auth_opts=("byacc-${version}.tgz.asc" "byacc-${version}.tgz")
