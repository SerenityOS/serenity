#!/usr/bin/env -S bash ../.port_include.sh
port=vim
version=8.2.2772
workdir="${port}-${version}"
useconfigure="true"
files="https://github.com/vim/vim/archive/refs/tags/v${version}.tar.gz vim-v${version}.tar.gz 47613400943bbf3e110c38e8c4923b9e51c1d63d9774313820e1d9b4c4bb9e11"
auth_type=sha256
configopts=("--with-tlib=tinfo" "--with-features=normal")
depends=("ncurses" "gettext")

export vim_cv_getcwd_broken=no
export vim_cv_memmove_handles_overlap=yes
export vim_cv_stat_ignores_slash=yes
export vim_cv_tgetent=zero
export vim_cv_terminfo=yes
export vim_cv_toupper_broken=no
export vim_cv_tty_group=world

post_install() {
    run ln -sf vim "${SERENITY_INSTALL_ROOT}/usr/local/bin/vi"
}
