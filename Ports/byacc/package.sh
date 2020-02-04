#!/bin/bash ../.port_include.sh
port=byacc
version=20191125
files="https://invisible-mirror.net/archives/byacc/byacc-${version}.tgz byacc-${version}.tgz
https://invisible-mirror.net/archives/byacc/byacc-${version}.tgz.asc byacc-${version}.tgz.asc"
useconfigure=true
auth_type="sig"
auth_import_key="C52048C0C0748FEE227D47A2702353E0F7E48EDB"
auth_opts="byacc-${version}.tgz.asc byacc-${version}.tgz"
