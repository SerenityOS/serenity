#!/usr/bin/env -S bash ../.port_include.sh
port=isl
version=0.24
useconfigure=true
use_fresh_config_sub=true
files=(
    "https://libisl.sourceforge.io/isl-${version}.tar.xz 043105cc544f416b48736fff8caf077fb0663a717d06b1113f16e391ac99ebad"
)
depends=("gmp")

if [ "$SERENITY_TOOLCHAIN" = "Clang" ]; then
    # This test fails with Clang because it doesn't let you take the address of compiler intrinsics.
    export ac_cv_have_decl___builtin_ffs=yes
fi
