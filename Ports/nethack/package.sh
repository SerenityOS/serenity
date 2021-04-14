#!/usr/bin/env -S bash ../.port_include.sh
port=nethack
version=3.6.6
workdir=NetHack-NetHack-${version}_Released
files="https://www.nethack.org/download/${version}/nethack-${version//.}-src.tgz nethack-${version//.}-src.tgz"
depends="ncurses bash"

build() {
    run sys/unix/setup.sh sys/unix/hints/serenity
    if [ ! -f ${workdir}/util/makedefs.host ]; then
        host_env
        run make -C util makedefs $makeopts
        run cp util/makedefs util/makedefs.host
        run make -C util dgn_comp $makeopts
        run cp util/dgn_comp util/dgn_comp.host
        run make -C util lev_comp $makeopts
        run cp util/lev_comp util/lev_comp.host
        run make -C util dlb $makeopts
        run cp util/dlb util/dlb.host
        target_env
        run make clean
    fi
    run make $makeopts
}
