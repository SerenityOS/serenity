#!/bin/sh ../.port_include.sh
port=libarchive
version=3.4.0
useconfigure=true
files="https://libarchive.org/downloads/libarchive-3.4.0.tar.gz libarchive-3.4.0.tar.gz"
patchlevel=1

configure() {
    run ./"$configscript" --host=i686-pc-serenity $configopts
}
