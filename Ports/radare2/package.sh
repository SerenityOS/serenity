#!/usr/bin/env -S bash ../.port_include.sh
port='radare2'
version='5.7.6'
files=(
    "https://github.com/radareorg/radare2/archive/refs/tags/${version}.tar.gz#1e798504a978929803ac7d6e42530b06c44be7e1abb5842877a88d7a34d9fd8f"
)
useconfigure='true'
configopts=("--disable-debugger" "--disable-threads" "--with-ostype=serenityos" "--host=${SERENITY_ARCH}-serenityos")

pre_configure() {
    cat "${workdir}/dist/plugins-cfg/plugins.def.cfg" | sed -e 's,io.shm,,' > "${workdir}/plugins.cfg"
}
