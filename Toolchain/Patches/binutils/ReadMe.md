# Patches for binutils on SerenityOS

## `0001-Add-support-for-SerenityOS.patch`

Add support for SerenityOS

Teaches the assembler, BFD, and the linker about the SerenityOS target
triple.

We set '/' to not start a comment in GAS, as the QEMU port uses it for
division in constant expressions in assembly files (cf. as --divide).

`/usr/lib/Loader.so` is set as the default ELF interpreter.

On AArch64, we set `COMMONPAGESIZE` to enable RELRO support.

