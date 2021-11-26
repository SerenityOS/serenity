#!/bin/sh

SCRIPT_DIR="$(dirname "${0}")"

if [ -z "$SERENITY_ARCH" ]; then
    SERENITY_ARCH="i686"
fi

# Set this environment variable to override the default debugger.
#
if [ -z "$SERENITY_KERNEL_DEBUGGER" ]; then
    if [ "$SERENITY_ARCH" = "aarch64" ]; then
        # Prepend the toolchain aarch64 bin directory so we pick up GDB from there
        PATH="$SCRIPT_DIR/../Toolchain/Local/aarch64/bin:$PATH"
        SERENITY_KERNEL_DEBUGGER="aarch64-pc-serenity-gdb"
    else
        SERENITY_KERNEL_DEBUGGER="gdb"
    fi
fi

# The QEMU -s option (enabled by default in ./run) sets up a debugger
# remote on localhost:1234. So point our debugger there, and inform
# the debugger which binary to load symbols, etc from.
#
if [ "$SERENITY_ARCH" = "x86_64" ]; then
    gdb_arch=i386:x86-64
    prekernel_image=Prekernel64
    kernel_base=0x2000200000
elif [ "$SERENITY_ARCH" = "i686" ]; then
    gdb_arch=i386:intel
    prekernel_image=Prekernel32
    kernel_base=0xc0200000
elif [ "$SERENITY_ARCH" = "aarch64" ]; then
    gdb_arch=aarch64:armv8-r
    prekernel_image=Prekernel
    kernel_base=0xc0000000 # FIXME
fi

# FIXME: This doesn't work when running QEMU inside the WSL2 VM
if command -v wslpath >/dev/null; then
    gdb_host=$(powershell.exe "(Test-Connection -ComputerName (hostname) -Count 1).IPV4Address.IPAddressToString" | tr -d '\r\n')
else
    gdb_host=localhost
fi


exec $SERENITY_KERNEL_DEBUGGER \
    -ex "file $SCRIPT_DIR/../Build/${SERENITY_ARCH:-i686}/Kernel/Prekernel/$prekernel_image" \
    -ex "set confirm off" \
    -ex "directory $SCRIPT_DIR/../Build/${SERENITY_ARCH:-i686}/" \
    -ex "add-symbol-file $SCRIPT_DIR/../Build/${SERENITY_ARCH:-i686}/Kernel/Kernel -o $kernel_base" \
    -ex "set confirm on" \
    -ex "set arch $gdb_arch" \
    -ex "set print frame-arguments none" \
    -ex "target remote ${gdb_host}:1234" \
    -ex "source $SCRIPT_DIR/serenity_gdb.py" \
    -ex "layout asm" \
    -ex "fs next" \
    "$@"
