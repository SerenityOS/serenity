#!/usr/bin/env -S bash ../.port_include.sh
port='radare2'
version='5.7.0'
files="https://github.com/radareorg/radare2/archive/refs/tags/${version}.tar.gz radare2-${version}.tar.gz fe7ca861bf71dd3c4766a57f73fd97b216bcfde161720f949c05875df212976b"
auth_type='sha256'
useconfigure='true'
configopts=("--disable-debugger" "--disable-threads" "--with-ostype=serenityos" "--host=${SERENITY_ARCH}-serenityos")

pre_configure() {
	cp -f "${workdir}/dist/plugins-cfg/plugins.tiny.cfg" "${workdir}/plugins.cfg"
}
