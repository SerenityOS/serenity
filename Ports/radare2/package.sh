#!/usr/bin/env -S bash ../.port_include.sh
port='radare2'
description='radare2 reverse engineering framework'
version='5.7.6'
website='https://github.com/radareorg/radare2'
files="https://github.com/radareorg/radare2/archive/refs/tags/${version}.tar.gz radare2-${version}.tar.gz 1e798504a978929803ac7d6e42530b06c44be7e1abb5842877a88d7a34d9fd8f"
auth_type='sha256'
useconfigure='true'
configopts=("--disable-debugger" "--disable-threads" "--with-ostype=serenityos" "--host=${SERENITY_ARCH}-serenityos")

pre_configure() {
    cat "${workdir}/dist/plugins-cfg/plugins.def.cfg" | sed -e 's,io.shm,,' > "${workdir}/plugins.cfg"
}
