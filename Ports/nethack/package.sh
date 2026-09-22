#!/usr/bin/env -S bash ../.port_include.sh
port='nethack'
version='5.0.0'
workdir="NetHack-${version}"
files=(
    "https://www.nethack.org/download/${version}/nethack-${version//.}-src.tgz#2959b7886aac76185b90aea0c9f80d14343f604de0ae96b3dd2a760f7ab3bde9"
)
depends=(
    'ncurses'
    'bash'
)

build() {
    run sys/unix/setup.sh sys/unix/hints/serenity
    run make fetch-lua
    if [ ! -f ${workdir}/util/makedefs.host ]; then
        host_env
        run make -C util makedefs "${makeopts[@]}"
        run cp util/makedefs util/makedefs.host
        run make -C util dlb "${makeopts[@]}"
        run cp util/dlb util/dlb.host
        target_env
        run make clean
    fi
    run make "${makeopts[@]}"
}
