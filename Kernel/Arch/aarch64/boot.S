/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// In a specially-named text section so that the linker script can put it first in .text.
.section ".text.first"

// We expect that x0 contains a pointer to a flattened devicetree,
// which is also what the AArch64 Linux boot protocol requires.

.global start
.type start, @function
start:
    // Let only core 0 continue, put other cores to sleep.
    mrs x13, MPIDR_EL1
    and x13, x13, 0xff
    cbnz x13, halt

    // Set the stack pointer register to the location defined in the linker script.
    adrp x14, end_of_initial_stack
    add x14, x14, :lo12:end_of_initial_stack
    mov sp, x14

    // Clear BSS.
    adrp x14, start_of_bss
    add x14, x14, :lo12:start_of_bss
    adrp x15, end_of_bss
    add x15, x15, :lo12:end_of_bss
    cmp x14, x15
    b.ge .Lbss_clear_done
.Lbss_clear_loop:
    str xzr, [x14], #8
    cmp x14, x15
    b.lt .Lbss_clear_loop
.Lbss_clear_done:

    b pre_init

halt:
    msr daifset, #2
    wfi
    b halt
