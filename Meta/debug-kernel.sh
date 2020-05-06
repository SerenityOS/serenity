#!/bin/sh

# Set this environment variable to override the default debugger.
#
[ -z "$SERENITY_KERNEL_DEBUGGER" ] && SERENITY_KERNEL_DEBUGGER="gdb"

# The QEMU -s option (enabled by default in ./run) sets up a debugger
# remote on localhost:1234. So point our debugger there, and inform
# the debugger which binary to load symbols, etc from.
#
$SERENITY_KERNEL_DEBUGGER \
    -ex "file $(pwd)/kernel" \
    -ex 'set arch i386:intel' \
    -ex 'target remote localhost:1234'
