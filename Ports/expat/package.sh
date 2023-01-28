#!/usr/bin/env -S bash ../.port_include.sh
port='expat'
description='Expat XML parser'
version='2.5.0'
website='https://libexpat.github.io/'
versionpath='2_5_0'
useconfigure='true'
files="https://github.com/libexpat/libexpat/releases/download/R_${versionpath}/expat-${version}.tar.xz expat-${version}.tar.xz ef2420f0232c087801abf705e89ae65f6257df6b7931d37846a193ef2e8cdcbe"
auth_type='sha256'
