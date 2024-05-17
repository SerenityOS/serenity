#!/usr/bin/env bash

SCRIPT_DIR="$(dirname "${0}")"

if [ -z "$SERENITY_ARCH" ]; then
    SERENITY_ARCH="x86_64"
fi

# Set this environment variable to override the default debugger.
#
if [ -z "$SERENITY_KERNEL_DEBUGGER" ]; then
    # Prepend the toolchain's GDB bin directory so we pick up GDB from there
    PATH="$SCRIPT_DIR/../Toolchain/Local/$SERENITY_ARCH-gdb/bin:$PATH"
    # GDB used to be installed directly inside the toolchain bin directory
    PATH="$SCRIPT_DIR/../Toolchain/Local/$SERENITY_ARCH/bin:$PATH"

    if command -v "$SERENITY_ARCH-pc-serenity-gdb" >/dev/null; then
        SERENITY_KERNEL_DEBUGGER="$SERENITY_ARCH-pc-serenity-gdb"
    elif command -v "$SERENITY_ARCH-elf-gdb" >/dev/null; then
        SERENITY_KERNEL_DEBUGGER="$SERENITY_ARCH-elf-gdb"
    elif command -v gdb >/dev/null && gdb -ex 'set architecture' -ex 'quit' 2>&1 | grep "${SERENITY_ARCH//_/-}"; then
        SERENITY_KERNEL_DEBUGGER="gdb"
    else
        echo "Error: No suitable GDB installation found." >&2
        echo "Please install $SERENITY_ARCH-elf-gdb or build it with Toolchain/BuildGDB.sh $SERENITY_ARCH" >&2
        # Prevent tmux from dying instantly by waiting for user input
        read -rp "Press Enter to exit"
        exit 1
    fi
fi

toolchain_suffix=
if [ "$SERENITY_TOOLCHAIN" = "Clang" ]; then
    toolchain_suffix="clang"
fi

# The QEMU -s option (enabled by default in ./run) sets up a debugger
# remote on localhost:1234. So point our debugger there, and inform
# the debugger which binary to load symbols, etc from.
#
if [ "$SERENITY_ARCH" = "x86_64" ]; then
    gdb_arch=i386:x86-64
    prekernel_image=Prekernel64
    kernel_image=Kernel_shared_object
    kernel_base=0x2000200000
elif [ "$SERENITY_ARCH" = "aarch64" ]; then
    gdb_arch=aarch64:armv8-r
    prekernel_image=Prekernel
    kernel_image=Kernel
    kernel_base=0x0
elif [ "$SERENITY_ARCH" = "riscv64" ]; then
    gdb_arch=riscv:rv64
    prekernel_image=Prekernel
    kernel_image=Kernel
    kernel_base=0x0
fi

# FIXME: This doesn't work when running QEMU inside the WSL2 VM
if command -v wslpath >/dev/null; then
    gdb_host=$(powershell.exe "(Test-Connection -ComputerName (hostname) -Count 1).IPV4Address.IPAddressToString" | tr -d '\r\n')
else
    gdb_host=${SERENITY_HOST_IP:-127.0.0.1}
fi


exec $SERENITY_KERNEL_DEBUGGER \
    -ex "file $SCRIPT_DIR/../Build/${SERENITY_ARCH:-x86_64}$toolchain_suffix/Kernel/Prekernel/$prekernel_image" \
    -ex "set confirm off" \
    -ex "directory $SCRIPT_DIR/../Build/${SERENITY_ARCH:-x86_64}$toolchain_suffix/" \
    -ex "add-symbol-file $SCRIPT_DIR/../Build/${SERENITY_ARCH:-x86_64}$toolchain_suffix/Kernel/$kernel_image -o $kernel_base" \
    -ex "set confirm on" \
    -ex "set arch $gdb_arch" \
    -ex "set print frame-arguments none" \
    -ex "set print asm-demangle on" \
    -ex "target remote ${gdb_host}:1234" \
    -ex "source $SCRIPT_DIR/serenity_gdb.py" \
    -ex "layout asm" \
    -ex "fs next" \
    "$@"
