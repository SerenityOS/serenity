#!/bin/sh ../.port_include.sh
port=libexpat
version=2.2.9
useconfigure=true
files="https://codeload.github.com/libexpat/libexpat/tar.gz/R_2_2_9 expat-2.2.9.tar.gz"
workdir=libexpat-R_2_2_9/expat/

configure() {
    run ./buildconf.sh
    run patch -p 0 < fix-autoconf.patch
    run ./"$configscript" --host=i686-pc-serenity $configopts
}
