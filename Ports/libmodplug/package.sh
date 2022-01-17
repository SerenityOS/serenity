#!/usr/bin/env -S bash ../.port_include.sh
port=libmodplug
version=0.8.8.5
useconfigure=true
use_fresh_config_sub=true
configopts=("ac_cv_c_bigendian=no")
files="https://download.sourceforge.net/modplug-xmms/libmodplug-${version}.tar.gz libmodplug-${version}.tar.gz 77462d12ee99476c8645cb5511363e3906b88b33a6b54362b4dbc0f39aa2daad"
auth_type=sha256
workdir="libmodplug-$version"

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    MODPLUG_LIBDIR="${SERENITY_INSTALL_ROOT}/usr/local/lib"
    ${CC} -shared -o ${MODPLUG_LIBDIR}/libmodplug.so.1 -Wl,-soname,libmodplug.so -Wl,--whole-archive ${MODPLUG_LIBDIR}/libmodplug.a -Wl,--no-whole-archive
    ln -rsf ${MODPLUG_LIBDIR}/libmodplug.so.1 ${MODPLUG_LIBDIR}/libmodplug.so
    rm -f ${MODPLUG_LIBDIR}/libmodplug.la
}
