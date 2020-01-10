#!/bin/bash ../.toolchain_include.sh

package=serenity-libc
version=
sourcedir=LibC
builddir=$sourcedir

fetch() {
    ln -sf $SERENITY_ROOT/Libraries/$sourcedir .
    run_builddir $MAKE clean
}

postinstall() {
    run_builddir $MAKE clean
}