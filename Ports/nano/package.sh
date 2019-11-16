#!/bin/bash ../.port_include.sh
port=nano
version=4.5
workdir=nano-4.5
useconfigure="true"
curlopts="-L"
files="https://www.nano-editor.org/dist/v4/nano-4.5.tar.xz nano-4.5.tar.xz"
configopts="--target=i686-pc-serenity --disable-browser --disable-utf8"
depends="ncurses"

export CPPFLAGS=-I${SERENITY_ROOT}/Root/usr/local/include/ncurses
