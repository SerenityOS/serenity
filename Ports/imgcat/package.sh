#!/usr/bin/env -S bash ../.port_include.sh
port=imgcat
version=2.5.0
useconfigure=false
files="https://github.com/eddieantonio/imgcat/releases/download/v${version}/imgcat-${version}.tar.gz imgcat-v${version}.tar.gz 1b5d45ccafc6fbb7a7ee685d4a5110101418d48b"
auth_type=md5

depends="ncurses"
