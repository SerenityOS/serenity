# Patches for binutils on SerenityOS

## `binutils.patch`

Add support for SerenityOS

Teaches the assembler, BFD, and the linker about the SerenityOS target
triple.

On x86_64, we override the default base address of non-PIE executables,
because the default (0x400000) is too close to the beginning of the
address space, and DynamicLoader often ends up allocating internal data
at that address. See commit 292398b5857d0104f7c33fdb5d79f45fe8b395dd.

