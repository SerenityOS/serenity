#!/usr/bin/env -S bash ../.port_include.sh
port=tinycc
version=dev
files="https://github.com/TinyCC/tinycc/archive/dev.tar.gz tinycc-dev.tar.gz"
useconfigure=true
makeopts=tcc

configure() {
    run ./configure --cross-prefix=i686-pc-serenity- --cpu=i686 --triplet=i686-pc-serenity --crtprefix=/usr/lib
}

export CONFIG_ldl=no
