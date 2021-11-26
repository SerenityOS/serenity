#!/usr/bin/env -S bash ../.port_include.sh
port=gnupg
version=2.3.0
useconfigure=true
configopts=("--with-libgpg-error-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
  "--with-libgcrypt-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
  "--with-libassuan-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
  "--with-ntbtls-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
  "--with-npth-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
  "--disable-dirmngr")
files="https://gnupg.org/ftp/gcrypt/gnupg/gnupg-${version}.tar.bz2 gnupg-${version}.tar.bz2 84c1ef39e8621cfb70f31463a5d1d8edeab44332bc1e0e1af9b78b6f9ed05bb4"
auth_type=sha256
depends=("libiconv" "libgpg-error" "libgcrypt" "libksba" "libassuan" "npth" "ntbtls")

pre_configure() {
    export GPGRT_CONFIG="${SERENITY_INSTALL_ROOT}/usr/local/bin/gpgrt-config"
    export CFLAGS="-L${SERENITY_INSTALL_ROOT}/usr/local/include"
    export LDFLAGS="-L${SERENITY_INSTALL_ROOT}/usr/local/lib -lm -liconv -ldl"
}

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" "${configopts[@]}"
}
