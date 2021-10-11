#!/usr/bin/env -S bash ../.port_include.sh
port=libmodplug
version=0.8.8.5
useconfigure=true
configopts=("ac_cv_c_bigendian=no")
files="https://download.sourceforge.net/modplug-xmms/libmodplug-${version}.tar.gz libmodplug-${version}.tar.gz 77462d12ee99476c8645cb5511363e3906b88b33a6b54362b4dbc0f39aa2daad"
auth_type=sha256
workdir="libmodplug-$version"

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libmodplug.so -Wl,-soname,libmodplug.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libmodplug.a -Wl,--no-whole-archive
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libmodplug.la
}
