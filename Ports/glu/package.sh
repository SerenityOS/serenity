#!/usr/bin/env -S bash ../.port_include.sh
port=glu
useconfigure="true"
version="9.0.2"
workdir="glu-glu-${version}"
files="https://gitlab.freedesktop.org/mesa/glu/-/archive/glu-${version}/glu-glu-${version}.tar.gz glu-glu-${version}.tar.gz 332d93a16376bc007e8232a8e5534da84e548cf3db9de040442c47a21f4625ba"
auth_type=sha256
depends=("pkgconf")

pre_configure() {
    export ACLOCAL="aclocal -I${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"
    export GL_CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/include/LibGL"
    export GL_LIBS="-lgl"

    run libtoolize
    run aclocal
    run autoconf
    run automake --add-missing

    # Manual config.sub patch
    run cp config.sub config.sub.contents
    run mv -f config.sub.contents config.sub
    run sed -i 's/-haiku/-serenity/' config.sub
}

post_configure() {
    unset ACLOCAL
    unset GL_CFLAGS
    unset GL_LIBS
}
