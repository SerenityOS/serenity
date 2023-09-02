#!/usr/bin/env -S bash ../.port_include.sh

port=readline
version=8.1.2
useconfigure=true
config_sub_paths=("support/config.sub")
use_fresh_config_sub=true
files=(
    "https://ftpmirror.gnu.org/gnu/readline/readline-${version}.tar.gz#7589a2381a8419e68654a47623ce7dfcb756815c8fee726b98f90bf668af7bc6"
)
configopts=(
    "--disable-static"
    "--enable-shared"
)

post_install() {
    # readline specifies termcap as a dependency in its pkgconfig file, without checking if it exists.
    # Remove it manually to keep other ports from discarding readline because termcap is supposedly missing.
    sed_in_place '/^Requires.private:/s/termcap//' "${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig/readline.pc"
}
