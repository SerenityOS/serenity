#!/usr/bin/env -S bash ../.port_include.sh
port=libtheora
version=1.1.1
useconfigure=true
files="https://ftp.osuosl.org/pub/xiph/releases/theora/libtheora-${version}.tar.bz2 libtheora-${version}.tar.bz2 b6ae1ee2fa3d42ac489287d3ec34c5885730b1296f0801ae577a35193d3affbc"
auth_type="sha256"
depends=("libvorbis")
configopts=("--disable-examples")

build_shared() {
    local name=$1
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/${name}.so -Wl,-soname,${name}.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/${name}.a -Wl,--no-whole-archive
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/${name}.la
}

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    build_shared libtheora
    build_shared libtheoradec
    build_shared libtheoraenc
}
