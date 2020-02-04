#!/bin/bash ../.port_include.sh
port=mandoc
version=1.14.5
useconfigure=true
files="https://mandoc.bsd.lv/snapshots/mandoc-${version}.tar.gz mandoc-${version}.tar.gz 8219b42cb56fc07b2aa660574e6211ac38eefdbf21f41b698d3348793ba5d8f7"
depends="less pcre2 zlib"
auth_type="sha256"