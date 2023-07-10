#!/usr/bin/env -S bash ../.port_include.sh

port='zig'
version='0.11.0-dev.670+f7fea080b'
files='https://github.com/ziglang/zig-bootstrap/archive/b9a466fd23d7777e1b3b87d49074ce66370fb7b3.tar.gz zig-bootstrap-b9a466f.tar.gz 84cf91d727c53ef49220ea6b2864dae3bd48e5e5a73be95bf3672c38a72b0946'
workdir='zig-bootstrap-b9a466fd23d7777e1b3b87d49074ce66370fb7b3'

post_fetch() {
    run mkdir -p out
    run cp -r "${PORT_META_DIR}/scripts" out/
}

build() {
    host_env
    cd "${workdir}"
    ./build "${SERENITY_ARCH}-serenity-none" "native"
}

install() {
    zig_install_dir="${workdir}/out/zig-${SERENITY_ARCH}-serenity-none-native"

    mkdir -p "${DESTDIR}/usr/local/bin/."
    mkdir -p "${DESTDIR}/usr/local/lib/."
    cp -rv "${zig_install_dir}/bin/"* "${DESTDIR}/usr/local/bin/"
    cp -rv "${zig_install_dir}/lib/"* "${DESTDIR}/usr/local/lib/"
}
