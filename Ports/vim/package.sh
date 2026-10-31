#!/usr/bin/env -S bash ../.port_include.sh
port='vim'
version='9.2.1254'
useconfigure="true"
files=(
    "https://github.com/vim/vim/archive/refs/tags/v${version}.tar.gz#32f47ddfe2357c3fa7a0b16d327e57884a29d991a27ad2a64e01937b8d1a71e6"
)
configopts=("--with-tlib=tinfo" "--with-features=huge" "CFLAGS=-std=c17")
depends=("ncurses" "gettext")

export vim_cv_getcwd_broken=no
export vim_cv_memmove_handles_overlap=yes
export vim_cv_stat_ignores_slash=yes
export vim_cv_tgetent=zero
export vim_cv_terminfo=yes
export vim_cv_toupper_broken=no
export vim_cv_tty_group=world
export vim_cv_uname_output='SerenityOS'
export vim_cv_uname_r_output='1.0-dev'
export vim_cv_uname_m_output="${SERENITY_ARCH}"

post_install() {
    run ln -sf vim "${SERENITY_INSTALL_ROOT}/usr/local/bin/vi"
}
