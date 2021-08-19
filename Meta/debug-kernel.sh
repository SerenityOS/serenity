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
    prekernel_image=Prekernel64
    kernel_base=0x2000200000
else
    gdb_arch=i386:intel
    prekernel_image=Prekernel
    kernel_base=0xc0200000
fi

# FIXME: This doesn't work when running QEMU inside the WSL2 VM
if command -v wslpath >/dev/null; then
    gdb_host=$(powershell.exe "(Test-Connection -ComputerName (hostname) -Count 1).IPV4Address.IPAddressToString" | tr -d '\r\n')
else
    gdb_host=localhost
fi

exec $SERENITY_KERNEL_DEBUGGER \
    -ex "file $(dirname "$0")/../Build/${SERENITY_ARCH:-i686}/Kernel/Prekernel/$prekernel_image" \
    -ex "set confirm off" \
    -ex "directory $(dirname "$0")/../Build/${SERENITY_ARCH:-i686}/" \
    -ex "add-symbol-file $(dirname "$0")/../Build/${SERENITY_ARCH:-i686}/Kernel/Kernel -o $kernel_base" \
    -ex "set confirm on" \
    -ex "set arch $gdb_arch" \
    -ex "set print frame-arguments none" \
    -ex "target remote ${gdb_host}:1234" \
    -ex "source $(dirname "$0")/serenity_gdb.py" \
    -ex "layout asm" \
    -ex "fs next" \
    "$@"
