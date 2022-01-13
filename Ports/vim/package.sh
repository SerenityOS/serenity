#!/usr/bin/env -S bash ../.port_include.sh
port=vim
version=8.2.4066
workdir="${port}-${version}"
useconfigure="true"
files="https://github.com/vim/vim/archive/refs/tags/v${version}.tar.gz vim-v${version}.tar.gz a4efb11228876f67fb953804e9bfe961892914c33fd9c2993079f49109521ac0"
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
