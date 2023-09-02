#!/usr/bin/env -S bash ../.port_include.sh
port='cowsay'
version='3.04'
depends=('perl5')
useconfigure='false'
files=(
    "https://github.com/tnalpgge/rank-amateur-cowsay/archive/refs/tags/cowsay-${version}.tar.gz#d8b871332cfc1f0b6c16832ecca413ca0ac14d58626491a6733829e3d655878b"
)
workdir="rank-amateur-cowsay-cowsay-${version}/"

prefix='/usr/local'
bin_path="${prefix}/bin"
cow_path_prefix="${prefix}/share"
perl_executable="${bin_path}/perl"

build() {
    run_replace_in_file "s#%PREFIX%#${prefix}#" cowsay
    run_replace_in_file "s#%BANGPERL%#!${perl_executable}#" cowsay
}

install() {
    run_nocd mkdir -p "${SERENITY_INSTALL_ROOT}/${bin_path}/"
    run cp cowsay "${SERENITY_INSTALL_ROOT}/${bin_path}/"
    run_nocd mkdir -p "${SERENITY_INSTALL_ROOT}/${cow_path_prefix}/"
    run cp -r cows/ "${SERENITY_INSTALL_ROOT}/${cow_path_prefix}/"
    run_nocd ln -sf "${bin_path}/cowsay" "${SERENITY_INSTALL_ROOT}/${bin_path}/cowthink"
}
