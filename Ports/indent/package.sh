#!/usr/bin/env -S bash ../.port_include.sh
port=indent
version=2.2.11
files="https://ftp.gnu.org/gnu/indent/indent-${version}.tar.gz indent-${version}.tar.gz
https://ftp.gnu.org/gnu/indent/indent-${version}.tar.gz.sig indent-${version}.tar.gz.sig
https://ftp.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
useconfigure=true
auth_type="sig"
auth_opts="--keyring ./gnu-keyring.gpg indent-${version}.tar.gz.sig"

post_install() {
    man_dir="${SERENITY_BUILD_DIR}/Root/usr/local/share/man/man1/"
    run mkdir -p "${man_dir}"
    run cp man/indent.1 "${man_dir}"
}
