/*
 * Copyright (c) 2023-2024, Sönke Holz <sholz8530@gmail.com>
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
//   t0 = .got.plt[1] (DynamicObject*)
//   t1 = .got.plt offset
_plt_trampoline:
    // Save argument registers a0-a7, fa0-fa7 and ra.
    addi sp, sp, -(18 * 8)

    sd a0, 0*8(sp)
    sd a1, 1*8(sp)
    sd a2, 2*8(sp)
    sd a3, 3*8(sp)
    sd a4, 4*8(sp)
    sd a5, 5*8(sp)
    sd a6, 6*8(sp)
    sd a7, 7*8(sp)

    // NOTE: We only support ABI_FLEN=64 in LibELF/Validation.cpp,
    //       so we only save the lower 64 bits of the fa* registers.
    fsd fa0, 8*8(sp)
    fsd fa1, 9*8(sp)
    fsd fa2, 10*8(sp)
    fsd fa3, 11*8(sp)
    fsd fa4, 12*8(sp)
    fsd fa5, 13*8(sp)
    fsd fa6, 14*8(sp)
    fsd fa7, 15*8(sp)

    sd ra, 16*8(sp)

    // The DynamicObject* is in t0.
    mv a0, t0

    // GOT entries are 8 bytes, but sizeof(Elf64_Rela) == 24, so multiply
    // t1 by 3 to get the relocation offset.
    slli a1, t1, 1
    add a1, a1, t1

    call _fixup_plt_entry

    // Save the resolved function's address.
    mv t0, a0

    // Restore argument registers and ra.
    ld a0, 0*8(sp)
    ld a1, 1*8(sp)
    ld a2, 2*8(sp)
    ld a3, 3*8(sp)
    ld a4, 4*8(sp)
    ld a5, 5*8(sp)
    ld a6, 6*8(sp)
    ld a7, 7*8(sp)

    fld fa0, 8*8(sp)
    fld fa1, 9*8(sp)
    fld fa2, 10*8(sp)
    fld fa3, 11*8(sp)
    fld fa4, 12*8(sp)
    fld fa5, 13*8(sp)
    fld fa6, 14*8(sp)
    fld fa7, 15*8(sp)

    ld ra, 16*8(sp)

    addi sp, sp, 18 * 8

    // Jump to the resolved function.
    jr t0
