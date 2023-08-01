#!/usr/bin/env -S bash ../.port_include.sh
port='freetype'
version='2.13.0'
files="https://download.savannah.gnu.org/releases/freetype/freetype-${version}.tar.gz freetype-${version}.tar.gz a7aca0e532a276ea8d85bd31149f0a74c33d19c8d287116ef8f5f8357b4f1f80"
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=("builds/unix/config.sub")
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
    "--with-brotli=no"
    "--with-bzip2=no"
    "--with-zlib=no"
    "--with-harfbuzz=no"
    "--with-png=no"
)
