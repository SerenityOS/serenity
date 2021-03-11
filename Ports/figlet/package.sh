#!/usr/bin/env -S bash ../.port_include.sh
port=figlet
version=2.2.5
files="http://ftp.figlet.org/pub/figlet/program/unix/figlet-${version}.tar.gz figlet-${version}.tar.gz d88cb33a14f1469fff975d021ae2858e"
makeopts="CC=${CC} LD=${CC}"
