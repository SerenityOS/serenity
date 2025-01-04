#!/usr/bin/env -S bash ../.port_include.sh
port='isl'
version='0.27'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "https://libisl.sourceforge.io/isl-${version}.tar.xz#6d8babb59e7b672e8cb7870e874f3f7b813b6e00e6af3f8b04f7579965643d5c"
)
depends=(
    'gmp'
)

if [ "$SERENITY_TOOLCHAIN" = 'Clang' ]; then
    # This test fails with Clang because it doesn't let you take the address of compiler intrinsics.
    export ac_cv_have_decl___builtin_ffs='yes'
fi
