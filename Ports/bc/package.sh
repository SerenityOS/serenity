#!/bin/bash ../.port_include.sh
port=bc
version=2.5.1
curlopts="-L"
files="https://github.com/gavinhoward/bc/releases/download/2.5.1/bc-2.5.1.tar.xz bc-2.5.1.tar.xz"
useconfigure=true
configscript=configure.sh

configure() {
    run env HOSTCC=gcc ./"$configscript"
}
