# Patches for binutils on SerenityOS

## `0001-Add-support-for-SerenityOS.patch`

Add support for SerenityOS

Teaches the assembler, BFD, and the linker about the SerenityOS target
triple.

We set '/' to not start a comment in GAS, as the QEMU port uses it for
division in constant expressions in assembly files (cf. as --divide).

`/usr/lib/Loader.so` is set as the default ELF interpreter.

On AArch64, we set `COMMONPAGESIZE` to enable RELRO support.

## `0002-ld-Add-Bsymbolic-non-weak-functions.patch`

ld: Add -Bsymbolic-non-weak-functions

This option is a subset of -Bsymbolic-functions: only STB_GLOBAL are
considered. Vague linkage functions are STB_WEAK. A vague linkage
function may have different addresses in a -Bsymbolic-functions linked
shared object and outside the shared object.
-Bsymbolic-non-weak-functions can keep pointer equality while providing
most benefits: (a) fewer JUMP_SLOT (symbol lookups) (b) avoid PLT
entries for default visibility defined functions.

    PR 27871
include/
    * bfdlink.h (struct bfd_link_info): Add dynamic_weak_functions.
ld/
    * ldlex.h (enum option_values): Add OPTION_SYMBOLIC_NON_WEAK_FUNCTIONS.
    * lexsup.c (struct ld_options): Add -Bsymbolic-non-weak-functions.
    (enum symbolic_enum): Add symbolic_non_weak_functions.
    (parse_args): Handle -Bsymbolic-non-weak-functions.
    * ld.texi: Document -Bsymbolic-non-weak-functions.
    * NEWS: Mention -Bsymbolic-non-weak-functions.
    * testsuite/ld-elf/shared.exp: Add tests.
    * testsuite/ld-elf/symbolic-non-weak-func.s: New file.
    * testsuite/ld-elf/symbolic-non-weak-func-a.rd: Likewise.
    * testsuite/ld-elf/symbolic-non-weak-func-b.rd: Likewise.

