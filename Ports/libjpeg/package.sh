#!/usr/bin/env -S bash ../.port_include.sh
port=libjpeg
version=9d
useconfigure=true
files="https://ijg.org/files/jpegsrc.v${version}.tar.gz jpeg-${version}.tar.gz ad7e40dedc268f97c44e7ee3cd54548a"
auth_type="md5"
workdir="jpeg-$version"
