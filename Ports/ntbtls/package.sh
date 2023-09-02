#!/usr/bin/env -S bash ../.port_include.sh
port=ntbtls
version=0.2.0
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
depends=("libgpg-error" "libksba" "libgcrypt" "zlib")
files=(
    "https://gnupg.org/ftp/gcrypt/ntbtls/ntbtls-${version}.tar.bz2#649fe74a311d13e43b16b26ebaa91665ddb632925b73902592eac3ed30519e17"
)
configopts=(
    "--with-libgcrypt-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    "--with-libgpg-error-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    # It's documented as "--with-libksba-prefix" (note the "lib"), but if it is set it is
    # immediately overwritten by whatever is given through "--with-ksba-prefix",
    # EVEN IF the latter switch is not given, thus overwriting it with the empty string.
    "--with-ksba-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
)

pre_configure() {
    export ntbtls_cv_gcc_has_f_visibility=no
}

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" "${configopts[@]}"
}
