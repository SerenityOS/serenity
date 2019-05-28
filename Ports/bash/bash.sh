#!/bin/sh
MAKEOPTS=" " # bash will die weirdly if you try build with multiple jobs.
INSTALLOPTS=" "
PORT_DIR=bash
function fetch() {
    run_fetch_git "https://git.savannah.gnu.org/git/bash.git"

    # Add serenity as a system for configure
    run_patch configure-system.patch -p1

    # For some reason, the build fails due to FILE* being undefined without this.
    # This is probably a LibC bug, but work around it for now.
    run_patch include-stdio.patch -p1

    # Locale calls crash right now. LibC bug, probably.
    run_patch disable-locale.patch -p1
}
function configure() {
    run_configure_autotools --disable-nls --without-bash-malloc
}
function build() {
    # Avoid some broken cross compile tests...
    run_command perl -p -i -e "s/define GETCWD_BROKEN 1/undef GETCWD_BROKEN/" config.h
    run_command perl -p -i -e "s/define CAN_REDEFINE_GETENV 1/undef CAN_REDEFINE_GETENV/" config.h
    run_make
}
function install() {
    run_make_install DESTDIR="$SERENITY_ROOT"/Root
}
source ../.port_include.sh
