#!/usr/bin/env -S bash ../.port_include.sh
port='ssmtp'
version='2.64-11'
files=(
    "https://salsa.debian.org/debian/ssmtp/-/archive/debian/${version}/ssmtp-debian-${version}.tar.gz#82abe0fb54e2ee209e9f8745498ce5f4e35f2f2b1bf95bd5e6dfbcfc61a8ebe2"
)
useconfigure='true'
workdir="ssmtp-debian-${version}"
configopts=(
    '--enable-ssl'
    '--enable-md5auth'
    "--prefix=${SERENITY_INSTALL_ROOT}/usr/local"
)
depends=('openssl')

pre_patch() {
    # Debian released multiple patches that fix issues with ssmtp. But they also decided to replace openssl with gnutls for internal licencing reasons.
    # As we have a stable openssl port and no stable gnutls port, we skip that patch.
    run perl -n -i -e '/01-374327-use-gnutls.patch/ or print' debian/patches/series
    # We will also skip the solaris patch as it messes with `generate_config` we already messed with :^)
    run perl -n -i -e '/02-557725-solaris.patch/ or print' debian/patches/series
    run bash -c 'while IFS= read -r line; do git apply debian/patches/$line || true; done < debian/patches/series'
}

pre_configure() {
    run_replace_in_file 's#$LIBS -lssl"#$LIBS -lssl -lcrypto --library '"${SERENITY_INSTALL_ROOT}"'/usr/local/lib/"#' configure
}
