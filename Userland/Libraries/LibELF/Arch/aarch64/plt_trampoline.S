/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2023, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.p2align 4
.globl _plt_trampoline
.hidden _plt_trampoline
.type _plt_trampoline,@function

// This function is called by the PLT stub to resolve functions lazily at runtime.
// It saves off any argument registers that might be clobbered by the symbol
// resolution code, calls that, and then jumps to the resolved function.
//
// See section 9.3 "Procedure Linkage Table" of the AArch64 ELF ABI.
// https://github.com/ARM-software/abi-aa/blob/main/aaelf64/aaelf64.rst
//
// The calling convention is:
//   x16 = &got.plt[2]
//   x17 = &_plt_trampoline
//   [sp, #0] = &got.plt[relocation_index + 3]
//   [sp, #8] = return address
_plt_trampoline:
    mov x17, sp

    // Save argument registers: x0-x7, v0-v7.
    stp x0, x1, [sp, #-16]!
    stp x2, x3, [sp, #-16]!
    stp x4, x5, [sp, #-16]!
    stp x6, x7, [sp, #-16]!

    stp q0, q1, [sp, #-32]!
    stp q2, q3, [sp, #-32]!
    stp q4, q5, [sp, #-32]!
    stp q6, q7, [sp, #-32]!

    // Load DynamicObject* from got.plt[1].
    ldr x0, [x16, #-8]

    // Calculate the rela.plt relocation offset.
    ldr x2, [x17]
    sub x1, x2, x16
    sub x1, x1, #8
    // GOT entries are 8 bytes, but sizeof(Elf64_Rela) == 24, so multiply
    // by 3 to get the relocation offset.
    add x1, x1, x1, lsl #1

    bl _fixup_plt_entry

    // Save the resolved function's address.
    mov x16, x0

    // Restore argument registers.
    ldp q6, q7, [sp], #32
    ldp q4, q5, [sp], #32
    ldp q2, q3, [sp], #32
    ldp q0, q1, [sp], #32

    ldp x6, x7, [sp], #16
    ldp x4, x5, [sp], #16
    ldp x2, x3, [sp], #16
    ldp x0, x1, [sp], #16

    // Restore link register saved by the PLT stub.
    ldp xzr, x30, [sp], #16

    // Jump to the resolved function.
    br x16

