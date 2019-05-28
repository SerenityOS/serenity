#!/bin/sh
PORT_DIR=vim
INSTALLOPTS="DESTDIR=$SERENITY_ROOT/Root/"

function fetch() {
    run_fetch_git "https://github.com/vim/vim.git"
}

function configure() {
	run_send_to_file src/auto/config.cache "
	vim_cv_getcwd_broken=no
	vim_cv_memmove_handles_overlap=yes
	vim_cv_stat_ignores_slash=yes
	vim_cv_tgetent=zero
	vim_cv_terminfo=yes
	vim_cv_toupper_broken=no
	vim_cv_tty_group=world
	"
	run_configure_autotools --with-tlib=ncurses --with-features=small
}

function build() {
    run_make
}

function install() {
    run_make_install
}

source ../.port_include.sh
