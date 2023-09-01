#!/usr/bin/env -S bash ../.port_include.sh
port='isl'
version='0.26'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "https://libisl.sourceforge.io/isl-${version}.tar.xz#a0b5cb06d24f9fa9e77b55fabbe9a3c94a336190345c2555f9915bb38e976504"
)
depends=(
    'gmp'
)

if [ "$SERENITY_TOOLCHAIN" = 'Clang' ]; then
    # This test fails with Clang because it doesn't let you take the address of compiler intrinsics.
    export ac_cv_have_decl___builtin_ffs='yes'
fi
