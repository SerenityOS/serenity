#!/bin/bash ../.port_include.sh
port=flex
version=2.6.4
curlopts="-L"
files="https://github.com/westes/flex/releases/download/v2.6.4/flex-2.6.4.tar.gz flex-2.6.4.tar.gz"
useconfigure=true
configopts=--disable-bootstrap
depends="m4 pcre2"
