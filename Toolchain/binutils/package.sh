#!/bin/bash ../.toolchain_include.sh

package=binutils
version=2.33.1
useconfigure=true
configopts="--target=$TARGET --with-sysroot=\"$SYSROOT\" --enable-shared --disable-nls"

filename="binutils-${version}.tar.gz"
files="http://ftp.gnu.org/gnu/binutils/$filename $filename 1a6b16bcc926e312633fcc3fae14ba0a"


build() {
    mkdir -p "${builddir}"

    if [ "$(uname)" = "Darwin" ]; then
        # under macOS generated makefiles are not resolving the "intl"
        # dependency properly to allow linking its own copy of
        # libintl when building with --enable-shared.
        run_builddir $MAKE $PARALLEL_BUILD $makeopts || true
        run_builddir pushd intl && $MAKE $PARALLEL_BUILD all-yes && popd
    fi

    run_builddir $MAKE $PARALLEL_BUILD $makeopts || exit 1
}

