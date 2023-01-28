#!/usr/bin/env -S bash ../.port_include.sh
port=libjpeg
description='libjpeg'
version=9e
website='https://ijg.org/'
useconfigure=true
configopts=("--disable-static" "--enable-shared")
files="https://ijg.org/files/jpegsrc.v${version}.tar.gz jpeg-${version}.tar.gz 4077d6a6a75aeb01884f708919d25934c93305e49f7e3f36db9129320e6f4f3d"
auth_type=sha256
workdir="jpeg-$version"
