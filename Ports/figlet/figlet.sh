#!/bin/sh
PORT_DIR=figlet
INSTALLOPTS="DESTDIR=$SERENITY_ROOT/Root/"

fetch() {
    run_fetch_web "http://ftp.figlet.org/pub/figlet/program/unix/figlet-2.2.5.tar.gz"

    run_patch figlet-no-toilet-fonts.patch -p1
}

configure() {
    echo "No configure script"
}

build() {
    run_make CC=i686-pc-serenity-gcc LD=i686-pc-serenity-gcc
}

install() {
    run_make_install
}

. ../.port_include.sh
