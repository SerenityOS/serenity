#!/usr/bin/env -S bash ../.port_include.sh
port='radare2'
version='6.2.6'
files=(
    "https://github.com/radareorg/radare2/archive/refs/tags/${version}.tar.gz#ef5ae9a0785be28ff38ea61e3b1351232c068691868bab98613c67a8b7aa4872"
)
useconfigure='true'
configopts=(
    '--disable-debugger'
    '--without-qjs'
    "--host=${SERENITY_ARCH}-serenityos"
)

pre_configure() {
    cat "${workdir}/dist/plugins-cfg/plugins.def.cfg" | sed -e 's,io.shm,,' > "${workdir}/plugins.cfg"
}
