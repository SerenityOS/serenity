#!/usr/bin/env -S bash ../.port_include.sh
port='git'
version='2.39.0'
files="https://mirrors.edge.kernel.org/pub/software/scm/git/git-${version}.tar.xz git-${version}.tar.xz ba199b13fb5a99ca3dec917b0bd736bc0eb5a9df87737d435eddfdf10d69265b"
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
