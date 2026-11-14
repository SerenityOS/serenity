#!/usr/bin/env -S bash ../.port_include.sh
port='git'
version='2.56.1'
files=(
    "https://mirrors.edge.kernel.org/pub/software/scm/git/git-${version}.tar.xz#f0b0bf89264b9bd2e1c640363e7529643989da76281600473c50745cd34a607f"
)
useconfigure='true'
configopts=(
    "--with-lib=${SERENITY_INSTALL_ROOT}/usr/local"
    'CFLAGS=-DNO_IPV6 -std=c17'
)
depends=(
    'curl'
    'zlib'
)

build() {
    run make \
        "${makeopts[@]}" \
        CURL_LDFLAGS='-lcurl -lssl -lcrypto -lz'
    run make strip STRIP="${STRIP}"
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
