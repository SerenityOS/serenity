#!/bin/sh

# Set this environment variable to override the default debugger.
#
[ -z "$SERENITY_KERNEL_DEBUGGER" ] && SERENITY_KERNEL_DEBUGGER="gdb"

# The QEMU -s option (enabled by default in ./run) sets up a debugger
# remote on localhost:1234. So point our debugger there, and inform
# the debugger which binary to load symbols, etc from.
#
if [ "$SERENITY_ARCH" = "x86_64" ]; then
    gdb_arch=i386:x86-64
    kernel_binary=Kernel64
else
    gdb_arch=i386:intel
    kernel_binary=Kernel
fi

exec $SERENITY_KERNEL_DEBUGGER \
    -ex "file $(dirname "$0")/../Build/${SERENITY_ARCH:-i686}/Kernel/$kernel_binary" \
    -ex "set arch $gdb_arch" \
    -ex 'target remote localhost:1234' \
    -ex "source $(dirname "$0")/serenity_gdb.py" \
    "$@"
