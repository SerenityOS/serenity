#!/bin/bash ../.port_include.sh
port=termcap
version=1.3.1
useconfigure=true
configopts="--prefix=$SERENITY_ROOT/Root/usr"
files="https://ftp.gnu.org/gnu/termcap/termcap-${version}.tar.gz termcap-${version}.tar.gz"
