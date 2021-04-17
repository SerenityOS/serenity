#!/usr/bin/env -S bash ../.port_include.sh
port=vim
version=8.2.2772
workdir="${port}-${version}"
useconfigure="true"
files="https://github.com/vim/vim/archive/refs/tags/v${version}.tar.gz vim-v${version}.tar.gz 0dbd7323008c1d95d0396e119210630f"
auth_type=md5
configopts="--with-tlib=tinfo --with-features=normal"
depends="ncurses"

export vim_cv_getcwd_broken=no
export vim_cv_memmove_handles_overlap=yes
export vim_cv_stat_ignores_slash=yes
export vim_cv_tgetent=zero
export vim_cv_terminfo=yes
export vim_cv_toupper_broken=no
export vim_cv_tty_group=world
