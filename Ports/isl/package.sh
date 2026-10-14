#!/usr/bin/env -S bash ../.port_include.sh
port='isl'
version='0.28'
useconfigure='true'
files=(
    "https://libisl.sourceforge.io/isl-${version}.tar.xz#3dc31b8e1b18329e42d5dfbf84dd55e15c59b61569a2ab246f61497d9592f727"
)
depends=(
    'gmp'
)

if [ "$SERENITY_TOOLCHAIN" = 'Clang' ]; then
    # This test fails with Clang because it doesn't let you take the address of compiler intrinsics.
    export ac_cv_have_decl___builtin_ffs='yes'
fi
