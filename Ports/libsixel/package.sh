#!/usr/bin/env -S bash ../.port_include.sh
port='libsixel'
version='1.8.7'
files=(
    "https://github.com/saitoha/libsixel/archive/refs/tags/v${version}.tar.gz#ebed621b4820f96ff428127797d94f4671f2393560acd4bcc8b540809d391510"
)
useconfigure='true'
configopts=("--prefix=${SERENITY_INSTALL_ROOT}/usr/local")

configure() {
    export ac_cv_func_malloc_0_nonnull=yes
    export ac_cv_func_realloc_0_nonnull=yes
    run ./configure --host="${SERENITY_ARCH}-serenity" "${configopts[@]}"
}

install() {
    run make install
}
