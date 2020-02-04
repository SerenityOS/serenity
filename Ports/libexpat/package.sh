#!/bin/bash ../.port_include.sh
port=libexpat
version=2.2.9
useconfigure=true
files="https://github.com/libexpat/libexpat/releases/download/R_2_2_9/expat-${version}.tar.xz expat-${version}.tar.xz
https://github.com/libexpat/libexpat/releases/download/R_2_2_9/expat-${version}.tar.xz.asc expat-${version}.tar.xz.asc"
workdir=expat-${version}
auth_type="sig"
auth_import_key="CB8DE70A90CFBF6C3BF5CC5696262ACFFBD3AEC6"
auth_opts="expat-${version}.tar.xz.asc expat-${version}.tar.xz"