#!/bin/bash ../.port_include.sh
port=mawk
version=1.3.4-20200120
files="https://invisible-mirror.net/archives/mawk/mawk-${version}.tgz mawk-${version}.tgz
https://invisible-mirror.net/archives/mawk/mawk-${version}.tgz.asc mawk-${version}.tgz.asc"
useconfigure=true
auth_type="sig"
auth_import_key="C52048C0C0748FEE227D47A2702353E0F7E48EDB"
auth_opts="mawk-${version}.tgz.asc mawk-${version}.tgz"
