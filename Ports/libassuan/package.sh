#!/usr/bin/env -S bash ../.port_include.sh
port='libassuan'
version='3.0.2'
useconfigure='true'
depends=(
    'libgpg-error'
)
files=(
    "https://gnupg.org/ftp/gcrypt/libassuan/libassuan-${version}.tar.bz2#d2931cdad266e633510f9970e1a2f346055e351bb19f9b78912475b8074c36f6"
)

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-serenity" \
        --build="$("${workdir}/build-aux/config.guess")" \
        --disable-static \
        --enable-shared \
        --with-libgpg-error-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --with-sysroot="${SERENITY_INSTALL_ROOT}" \
        LDFLAGS="-lintl"
}

build() {
    host_env
    run make -C src CC_FOR_BUILD="${HOST_CC}" mkheader
    target_env
    run make "${makeopts[@]}"
}
