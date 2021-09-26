#!/usr/bin/env -S bash ../.port_include.sh
port=radare2
version=5.4.0-git
useconfigure=true
workdir=$port-$version
files="https://codeload.github.com/radareorg/radare2/tar.gz/refs/tags/$version radare2-$version.tar.gz c5e98cd4ea011dde2f08e68144e98e85e82fe45eef92a17dedc06e9404da117e"
auth_type=sha256
configopts=("--disable-debugger" "--with-ostype=serenityos" "--host=i686-serenityos")

pre_configure() {
	cp -f "$workdir/dist/plugins-cfg/plugins.tiny.cfg" "$workdir/plugins.cfg"
}
