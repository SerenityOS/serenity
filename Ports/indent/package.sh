#!/usr/bin/env -S bash ../.port_include.sh
port='indent'
version='2.2.13'
files=(
    "https://ftpmirror.gnu.org/gnu/indent/indent-${version}.tar.gz#9e64634fc4ce6797b204bcb8897ce14fdd0ab48ca57696f78767c59cae578095"
)
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=("config/config.sub")

post_install() {
    man_dir="${SERENITY_INSTALL_ROOT}/usr/local/share/man/man1/"
    run mkdir -p "${man_dir}"
    run cp man/indent.1 "${man_dir}"
}
