#!/usr/bin/env -S bash ../.port_include.sh
port=radare2
version=5.7.0
useconfigure=true
workdir=$port-$version
files="https://codeload.github.com/radareorg/radare2/tar.gz/refs/tags/$version radare2-$version.tar.gz fe7ca861bf71dd3c4766a57f73fd97b216bcfde161720f949c05875df212976b"
auth_type=sha256
configopts=("--disable-debugger" "--disable-threads" "--with-ostype=serenityos" "--host=i686-serenityos")

pre_configure() {
	cp -f "$workdir/dist/plugins-cfg/plugins.tiny.cfg" "$workdir/plugins.cfg"
}
