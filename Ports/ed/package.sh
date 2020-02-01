#!/bin/bash ../.port_include.sh
port=ed
version=1.15
files="https://ftp.gnu.org/gnu/ed/ed-1.15.tar.lz ed-1.15.tar.lz"
useconfigure=true
depends=pcre2

configure() {
	run ./"$configscript" CC=i686-pc-serenity-gcc
}
