#!/usr/bin/env -S bash ../.port_include.sh
port=vim
version=8.2.3800
workdir="${port}-${version}"
useconfigure="true"
files="https://github.com/vim/vim/archive/refs/tags/v${version}.tar.gz vim-v${version}.tar.gz 5580c31980558612e7a1f85d0d73402b3feacc8ff174a70554cd2d0a44cd2966"
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
export vim_cv_uname_output=SerenityOS
export vim_cv_uname_r_output=1.0-dev
export vim_cv_uname_m_output=$SERENITY_ARCH

post_install() {
    run ln -sf vim "${SERENITY_INSTALL_ROOT}/usr/local/bin/vi"
}
