#!/usr/bin/env -S bash ../.port_include.sh
port=git
version=2.34.1
useconfigure="true"
files="https://mirrors.edge.kernel.org/pub/software/scm/git/git-${version}.tar.xz git-${version}.tar.xz 3a0755dd1cfab71a24dd96df3498c29cd0acd13b04f3d08bf933e81286db802c"
auth_type=sha256
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--with-lib=${SERENITY_INSTALL_ROOT}/usr/local" "CFLAGS=-DNO_IPV6" "LDFLAGS=-L${SERENITY_INSTALL_ROOT}/usr/local/lib")
depends=("zlib" "curl")

build() {
    run make "${makeopts[@]}" CURL_LDFLAGS="-lcurl -lssl -lcrypto -lz"
    run make strip
}

post_install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/home/anon"
    run cp "../default_gitconfig" "${SERENITY_INSTALL_ROOT}/home/anon/.gitconfig"
}

export NO_PERL=YesPlease
export NO_PYTHON=YesPlease
export NO_EXPAT=YesPlease
export NO_TCLTK=YesPlease
export ac_cv_fread_reads_directories=no
export ac_cv_snprintf_returns_bogus=no
export ac_cv_iconv_omits_bom=no
export ac_cv_lib_curl_curl_global_init=yes
