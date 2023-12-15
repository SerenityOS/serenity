/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
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
// See section 8.4.6 "Program Linkage Table" of the RISC-V psABI.
// https://github.com/riscv-non-isa/riscv-elf-psabi-doc/releases/download/v1.0/riscv-abi.pdf
//
// The calling convention is:
//   t0 = DynamicObject*
//   t1 = .got.plt offset

_plt_trampoline:
    // Save argument registers (a0-a7) and ra. TODO: floating point regs
    addi sp, sp, -(10 * 8)
    sd ra, 0*8(sp)
    sd a0, 1*8(sp)
    sd a1, 2*8(sp)
    sd a2, 3*8(sp)
    sd a3, 4*8(sp)
    sd a4, 5*8(sp)
    sd a5, 6*8(sp)
    sd a6, 7*8(sp)
    sd a7, 8*8(sp)

    // DynamicObject* is in t0.
    mv a0, t0

    // GOT entries are 8 bytes, but sizeof(Elf64_Rela) == 24, so multiply
    // by 3 to get the relocation offset.
    slli a1, t1, 1
    add a1, a1, t1

    call _fixup_plt_entry

    // Save the resolved function's address.
    mv t0, a0

    // Restore argument registers and ra.
    ld ra, 0*8(sp)
    ld a0, 1*8(sp)
    ld a1, 2*8(sp)
    ld a2, 3*8(sp)
    ld a3, 4*8(sp)
    ld a4, 5*8(sp)
    ld a5, 6*8(sp)
    ld a6, 7*8(sp)
    ld a7, 8*8(sp)
    addi sp, sp, (10 * 8)

    // Jump to the resolved function.
    jr t0
