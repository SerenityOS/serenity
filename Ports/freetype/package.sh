#!/usr/bin/env -S bash ../.port_include.sh
port='freetype'
version='2.12.1'
files="https://download.savannah.gnu.org/releases/freetype/freetype-${version}.tar.gz freetype-${version}.tar.gz efe71fd4b8246f1b0b1b9bfca13cfff1c9ad85930340c27df469733bbb620938"
auth_type='sha256'
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

install() {
    run make DESTDIR="${SERENITY_INSTALL_ROOT}" "${installopts[@]}" install
    ${CC} -shared -o "${SERENITY_INSTALL_ROOT}/usr/local/lib/libfreetype.so" -Wl,-soname,libfreetype.so -Wl,--whole-archive "${SERENITY_INSTALL_ROOT}/usr/local/lib/libfreetype.a" -Wl,--no-whole-archive
    rm -f "${SERENITY_INSTALL_ROOT}/usr/local/lib/libfreetype.la"
}
