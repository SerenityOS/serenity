#!/usr/bin/env -S bash ../.port_include.sh
port=libksba
version=1.5.1
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
depends=("libgpg-error")
files=(
    "https://gnupg.org/ftp/gcrypt/libksba/libksba-${version}.tar.bz2#b0f4c65e4e447d9a2349f6b8c0e77a28be9531e4548ba02c545d1f46dc7bf921"
)
configopts=(
    "--with-libgpg-error-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
)

pre_configure() {
    export ksba_cv_gcc_has_f_visibility=no
}

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" "${configopts[@]}"
}
