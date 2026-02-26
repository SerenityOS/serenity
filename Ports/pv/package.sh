#!/usr/bin/env -S bash ../.port_include.sh
port=pv
version=1.10.3
useconfigure=true
configopts=("--disable-nls")
files=(
    "https://ivarch.com/s/pv-${version}.tar.gz#aa1630c79af6960a8922ffa64d2c3e7f87486da21fcb57e277824294fd266742"
)

# Map LD to the serenity Toolchain, otherwise the host LD is used.
if [ "$SERENITY_TOOLCHAIN" = "Clang" ]; then
    export LD=$(CC)
else
    export LD="${SERENITY_ARCH}-pc-serenity-ld"
fi
