#!/usr/bin/env -S bash ../.port_include.sh
port='vim'
version='9.2.1202'
useconfigure="true"
files=(
    "https://github.com/vim/vim/archive/refs/tags/v${version}.tar.gz#9e97414b85bdcb2d28d5264c65dd730d86b9a75319b005c63da0bc130c715a2f"
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
