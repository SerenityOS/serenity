#!/usr/bin/env -S bash ../.port_include.sh

port="libnbcompat"
version="20210710"
useconfigure="true"
workdir=${port}
depends="bmake"
configopts="--enable-db"

fetch() {
	true
}

build() {
	run bmake
}

install() {
	run bmake install DESTDIR=${DESTDIR}
}
