#!/usr/bin/env -S bash ../.port_include.sh
port='diffutils'
version='3.12'
depends=(
    'libiconv'
)
files=(
    "https://ftpmirror.gnu.org/gnu/diffutils/diffutils-${version}.tar.xz#7c8b7f9fc8609141fdea9cece85249d308624391ff61dedaf528fcb337727dfd"
)
useconfigure='true'
configopts=(
    # [diffutils/configure] checking whether strcasecmp works...
    # [diffutils/configure] configure: error: cannot run test program while cross compiling
    'gl_cv_func_strcasecmp_works=yes'
)
