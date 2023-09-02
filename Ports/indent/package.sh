#!/usr/bin/env -S bash ../.port_include.sh
port=indent
version=2.2.11
files=(
    "https://ftpmirror.gnu.org/gnu/indent/indent-${version}.tar.gz#aaff60ce4d255efb985f0eb78cca4d1ad766c6e051666073050656b6753a0893"
)
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("config/config.sub")

post_install() {
    man_dir="${SERENITY_INSTALL_ROOT}/usr/local/share/man/man1/"
    run mkdir -p "${man_dir}"
    run cp man/indent.1 "${man_dir}"
}
