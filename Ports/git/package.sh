#!/usr/bin/env -S bash ../.port_include.sh
port='git'
version='2.36.1'
files="https://mirrors.edge.kernel.org/pub/software/scm/git/git-${version}.tar.xz git-${version}.tar.xz 405d4a0ff6e818d1f12b3e92e1ac060f612adcb454f6299f70583058cb508370"
auth_type='sha256'
useconfigure='true'
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--with-lib=${SERENITY_INSTALL_ROOT}/usr/local" "CFLAGS=-DNO_IPV6" "LDFLAGS=-L${SERENITY_INSTALL_ROOT}/usr/local/lib")
depends=("zlib" "curl")

build() {
    run make "${makeopts[@]}" CURL_LDFLAGS="-lcurl -lssl -lcrypto -lz"
    run make strip
}

post_install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/home/anon"
    cat << 'EOF' > "${SERENITY_INSTALL_ROOT}/home/anon/.gitconfig"
[core]
    editor = TextEditor
    pager = less

[user]
    email = anon
    name = anon
EOF
}

export NO_PERL=YesPlease
export NO_PYTHON=YesPlease
export NO_EXPAT=YesPlease
export NO_TCLTK=YesPlease
export ac_cv_fread_reads_directories=no
export ac_cv_snprintf_returns_bogus=no
export ac_cv_iconv_omits_bom=no
export ac_cv_lib_curl_curl_global_init=yes
