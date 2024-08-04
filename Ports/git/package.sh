#!/usr/bin/env -S bash ../.port_include.sh
port='git'
version='2.46.0'
files=(
    "https://mirrors.edge.kernel.org/pub/software/scm/git/git-${version}.tar.xz#7f123462a28b7ca3ebe2607485f7168554c2b10dfc155c7ec46300666ac27f95"
)
useconfigure='true'
configopts=(
    "--target=${SERENITY_ARCH}-pc-serenity"
    "--with-lib=${SERENITY_INSTALL_ROOT}/usr/local"
    'CFLAGS=-DNO_IPV6'
)
depends=(
    'curl'
    'zlib'
)

build() {
    run make \
        "${makeopts[@]}" \
        CURL_LDFLAGS='-lcurl -lssl -lcrypto -lz'
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

export NO_PERL='YesPlease'
export NO_PYTHON='YesPlease'
export NO_EXPAT='YesPlease'
export NO_TCLTK='YesPlease'
export ac_cv_fread_reads_directories='no'
export ac_cv_snprintf_returns_bogus='no'
export ac_cv_iconv_omits_bom='no'
export ac_cv_lib_curl_curl_global_init='yes'
