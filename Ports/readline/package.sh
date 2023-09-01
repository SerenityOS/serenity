#!/usr/bin/env -S bash ../.port_include.sh
port='readline'
version='8.2'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'support/config.sub'
)
files=(
    "https://ftpmirror.gnu.org/gnu/readline/readline-${version}.tar.gz#3feb7171f16a84ee82ca18a36d7b9be109a52c04f492a053331d7d1095007c35"
)
configopts=(
    '--disable-static'
    '--enable-shared'
)

post_install() {
    # readline specifies termcap as a dependency in its pkgconfig file, without checking if it exists.
    # Remove it manually to keep other ports from discarding readline because termcap is supposedly missing.
    sed_in_place '/^Requires.private:/s/termcap//' "${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig/readline.pc"
}
