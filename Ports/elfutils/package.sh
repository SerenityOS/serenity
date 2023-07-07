#!/usr/bin/env -S bash ../.port_include.sh
port='elfutils'
version='0.186'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('config/config.sub')
files="http://archive.ubuntu.com/ubuntu/pool/main/e/elfutils/elfutils_${version}.orig.tar.bz2 elfutils_${version}.orig.tar.bz2 7f6fb9149b1673d38d9178a0d3e0fb8a1ec4f53a9f4c2ff89469609879641177"
auth_type='sha256'
depends=('zlib' 'libfts' 'argp-standalone' 'musl-obstack' 'curl' 'libarchive' 'sqlite' 'gettext')
configopts=(
    "--disable-debuginfod"
    "--enable-libdebuginfod=dummy"
    "--disable-libdwfl"
    "--enable-maintainer-mode"
)

pre_configure() {
    # Rebuild after patching configure.ac to support serenity host.

    # `automake` may exit with a warning about how there is a mismatch
    # between the versions of autoconf and automake that were previously
    # used to generate aclocal and specifed in configure.ac.
    run automake --add-missing || true
    run autoreconf
}

export LIBC="musl"
export LDFLAGS='-lintl -lobstack'
export CFLAGS="-Wno-discarded-qualifiers -Wno-unused-value"

install() {
    run cp src/nm "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp src/size "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp src/elflint "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp src/findtextrel "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp src/addr2line "${SERENITY_INSTALL_ROOT}/usr/local/bin"
	run cp src/elfcmp "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp src/objdump "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp src/ranlib "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp src/stack "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp src/elfcompress "${SERENITY_INSTALL_ROOT}/usr/local/bin"
	run cp src/elfclassify "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
