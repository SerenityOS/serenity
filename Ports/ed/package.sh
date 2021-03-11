#!/usr/bin/env -S bash ../.port_include.sh
port=ed
version=1.15
files="https://ftp.gnu.org/gnu/ed/ed-${version}.tar.lz ed-${version}.tar.lz"
useconfigure=true
depends=pcre2

configure() {
	run ./"$configscript"
}
