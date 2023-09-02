#!/usr/bin/env -S bash ../.port_include.sh
port='expat'
version='2.5.0'
versionpath='2_5_0'
useconfigure='true'
files=(
    "https://github.com/libexpat/libexpat/releases/download/R_${versionpath}/expat-${version}.tar.xz#ef2420f0232c087801abf705e89ae65f6257df6b7931d37846a193ef2e8cdcbe"
)
