#!/usr/bin/env -S bash ../.port_include.sh
port=libmodplug
version=0.8.9.0
useconfigure=true
use_fresh_config_sub=true
configopts=("ac_cv_c_bigendian=no")
files="https://download.sourceforge.net/modplug-xmms/libmodplug-${version}.tar.gz libmodplug-${version}.tar.gz 457ca5a6c179656d66c01505c0d95fafaead4329b9dbaa0f997d00a3508ad9de"
auth_type=sha256
workdir="libmodplug-$version"

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    MODPLUG_LIBDIR="${SERENITY_INSTALL_ROOT}/usr/local/lib"
    ${CC} -shared -o ${MODPLUG_LIBDIR}/libmodplug.so.1 -Wl,-soname,libmodplug.so -Wl,--whole-archive ${MODPLUG_LIBDIR}/libmodplug.a -Wl,--no-whole-archive
    ln -rsf ${MODPLUG_LIBDIR}/libmodplug.so.1 ${MODPLUG_LIBDIR}/libmodplug.so
    rm -f ${MODPLUG_LIBDIR}/libmodplug.la
}
